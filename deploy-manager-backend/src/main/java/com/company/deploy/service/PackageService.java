package com.company.deploy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.deploy.config.MinioConfig;
import com.company.deploy.dto.PackageProgressDTO;
import com.company.deploy.common.PageResult;
import com.company.deploy.entity.*;
import com.company.deploy.enums.FileTypeEnum;
import com.company.deploy.enums.PackageStatus;
import com.company.deploy.mapper.PackageRecordMapper;
import com.company.deploy.mapper.PackageTaskMapper;
import com.company.deploy.mapper.ProjectMapper;
import com.company.deploy.mapper.UploadedFileMapper;
import com.company.deploy.mapper.LicenseRecordMapper;
import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.PostConstruct;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

@Slf4j
@Service
@RequiredArgsConstructor
public class PackageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final ProjectMapper projectMapper;
    private final UploadedFileMapper uploadedFileMapper;
    private final PackageTaskMapper packageTaskMapper;
    private final PackageRecordMapper packageRecordMapper;
    private final LicenseRecordMapper licenseRecordMapper;
    private final ScriptTemplateService scriptTemplateService;

    @Value("${package.temp-dir:./temp/packages}")
    private String tempDir;

    @Value("${package.max-concurrent-tasks:2}")
    private int maxConcurrent;

    @Autowired
    @Lazy
    private PackageService self;

    private final Map<String, List<PackageProgressDTO.LogEntry>> taskLogs = new ConcurrentHashMap<>();
    private Semaphore packageSemaphore;
    private final ConcurrentHashMap<Long, String> activeProjectTasks = new ConcurrentHashMap<>();

    @PostConstruct
    public void initSemaphore() {
        packageSemaphore = new Semaphore(maxConcurrent);
        log.info("Package concurrency limit initialized: {}", maxConcurrent);
    }

    public String startPackage(Long projectId, String password, Long licenseId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new RuntimeException("\u9879\u76ee\u4e0d\u5b58\u5728");
        }

        if (!"READY".equals(project.getStatus())) {
            throw new RuntimeException("\u9879\u76ee\u72b6\u6001\u4e0d\u662f READY\uff0c\u65e0\u6cd5\u6253\u5305");
        }

        if (activeProjectTasks.containsKey(projectId)) {
            throw new RuntimeException("\u8be5\u9879\u76ee\u5df2\u6709\u6253\u5305\u4efb\u52a1\u5728\u8fdb\u884c\u4e2d\uff0c\u8bf7\u7b49\u5f85\u5b8c\u6210\u540e\u518d\u8bd5");
        }

        LambdaQueryWrapper<PackageTask> activeCheck = new LambdaQueryWrapper<>();
        activeCheck.eq(PackageTask::getProjectId, projectId)
                .in(PackageTask::getStatus, PackageStatus.PENDING.getCode(), PackageStatus.RUNNING.getCode());
        Long activeCount = packageTaskMapper.selectCount(activeCheck);
        if (activeCount != null && activeCount > 0) {
            throw new RuntimeException("\u8be5\u9879\u76ee\u5df2\u6709\u6253\u5305\u4efb\u52a1\u5728\u8fdb\u884c\u4e2d\uff0c\u8bf7\u7b49\u5f85\u5b8c\u6210\u540e\u518d\u8bd5");
        }

        String taskId = UUID.randomUUID().toString();

        PackageTask task = new PackageTask();
        task.setId(taskId);
        task.setProjectId(projectId);
        task.setStatus(PackageStatus.PENDING.getCode());
        task.setProgress(0);
        task.setCurrentStep("\u7b49\u5f85\u6253\u5305...");
        task.setPassword(password);
        task.setCreatedAt(LocalDateTime.now());
        packageTaskMapper.insert(task);

        activeProjectTasks.put(projectId, taskId);
        taskLogs.put(taskId, new ArrayList<>());
        addLog(taskId, "\u5f00\u59cb\u6253\u5305 " + project.getName() + "...", "INFO");

        self.executePackageAsync(taskId, projectId, password, licenseId);

        return taskId;
    }

    @Async("packageTaskExecutor")
    public void executePackageAsync(String taskId, Long projectId, String password, Long licenseId) {
        try {
            addLog(taskId, "\u6392\u961f\u7b49\u5f85\u6253\u5305\u8d44\u6e90...", "INFO");
            packageSemaphore.acquire();

            try {
                updateTaskStatus(taskId, PackageStatus.RUNNING, 0, "\u9a8c\u8bc1\u914d\u7f6e\u5b8c\u6574\u6027...");
                addLog(taskId, "\u9a8c\u8bc1\u914d\u7f6e\u5b8c\u6574\u6027...", "INFO");

                Project project = projectMapper.selectById(projectId);
                if (project == null) {
                    throw new RuntimeException("\u9879\u76ee\u4e0d\u5b58\u5728");
                }

                if (project.getJarFileId() == null) {
                    throw new RuntimeException("\u8bf7\u5148\u914d\u7f6eJAR\u5305");
                }

                Path packageDir = Paths.get(tempDir, taskId);
                Files.createDirectories(packageDir);
                Path zipPath = null;

                try {
                    updateTaskProgress(taskId, 5, "\u6536\u96c6\u540e\u7aefJAR\u5305...");
                    addLog(taskId, "\u6536\u96c6\u540e\u7aefJAR\u5305...", "INFO");
                    collectJar(project, packageDir);
                    addLog(taskId, "\u6536\u96c6\u540e\u7aefJAR\u5305... OK", "INFO");

                    updateTaskProgress(taskId, 15, "\u6536\u96c6JDK\u73af\u5883...");
                    addLog(taskId, "\u6536\u96c6JDK\u73af\u5883...", "INFO");
                    collectInfraComponentById(project.getJdkComponentId(), packageDir.resolve("jdk"), "JDK");
                    addLog(taskId, "\u6536\u96c6JDK\u73af\u5883... OK", "INFO");

                    updateTaskProgress(taskId, 30, "\u6536\u96c6Vue\u524d\u7aef + Nginx...");
                    addLog(taskId, "\u6536\u96c6Vue\u524d\u7aef + Nginx...", "INFO");
                    collectVue(project, packageDir);
                    if (Boolean.TRUE.equals(project.getIncludeNginx())) {
                        collectInfraComponentById(project.getNginxComponentId(), packageDir.resolve("nginx"), "Nginx");
                        collectNginxConf(project, packageDir);
                    }
                    addLog(taskId, "\u6536\u96c6Vue\u524d\u7aef + Nginx... OK", "INFO");

                    if (Boolean.TRUE.equals(project.getIncludeMysql())) {
                        updateTaskProgress(taskId, 45, "\u6536\u96c6MySQL\u4fbf\u643a\u5305...");
                        addLog(taskId, "\u6536\u96c6MySQL\u4fbf\u643a\u5305...", "INFO");
                        collectInfraComponentById(project.getMysqlComponentId(), packageDir.resolve("mysql"), "MySQL");
                        addLog(taskId, "\u6536\u96c6MySQL\u4fbf\u643a\u5305... OK", "INFO");
                    }

                    if (Boolean.TRUE.equals(project.getIncludeMinio())) {
                        updateTaskProgress(taskId, 55, "\u6536\u96c6MinIO...");
                        addLog(taskId, "\u6536\u96c6MinIO...", "INFO");
                        collectInfraComponentById(project.getMinioComponentId(), packageDir.resolve("minio"), "MinIO");
                        addLog(taskId, "\u6536\u96c6MinIO... OK", "INFO");
                    }

                    if (Boolean.TRUE.equals(project.getIncludeRedis())) {
                        updateTaskProgress(taskId, 58, "\u6536\u96c6Redis...");
                        addLog(taskId, "\u6536\u96c6Redis...", "INFO");
                        collectInfraComponentById(project.getRedisComponentId(), packageDir.resolve("redis"), "Redis");
                        addLog(taskId, "\u6536\u96c6Redis... OK", "INFO");
                    }

                    if (Boolean.TRUE.equals(project.getEngineEnabled())) {
                        updateTaskProgress(taskId, 62, "\u6536\u96c6\u5f15\u64ce...");
                        addLog(taskId, "\u6536\u96c6\u5f15\u64ce...", "INFO");
                        collectInfraComponentById(project.getEngineComponentId(), packageDir.resolve("engine"), "\u5f15\u64ce");
                        addLog(taskId, "\u6536\u96c6\u5f15\u64ce... OK", "INFO");
                    }

                    updateTaskProgress(taskId, 70, "\u751f\u6210\u542f\u52a8\u811a\u672c\u548c\u5e94\u7528\u914d\u7f6e...");
                    addLog(taskId, "\u751f\u6210\u542f\u52a8\u811a\u672c\u548c\u5e94\u7528\u914d\u7f6e...", "INFO");
                    scriptTemplateService.generateScripts(project, packageDir);
                    generateAppConfig(project, packageDir);
                    addLog(taskId, "\u751f\u6210\u542f\u52a8\u811a\u672c\u548c\u5e94\u7528\u914d\u7f6e... OK", "INFO");

                    // Step 7.5: Inject License (after scripts, before ZIP)
                    if (licenseId != null) {
                        updateTaskProgress(taskId, 75, "\u6ce8\u5165License...");
                        addLog(taskId, "\u6ce8\u5165License...", "INFO");
                        injectLicense(licenseId, packageDir);
                        addLog(taskId, "\u6ce8\u5165License... OK", "INFO");
                    }

                    updateTaskProgress(taskId, 80, "\u6b63\u5728\u6253\u5305\u52a0\u5bc6ZIP...");
                    addLog(taskId, "\u6b63\u5728\u6253\u5305\u52a0\u5bc6ZIP...", "INFO");
                    zipPath = createEncryptedZip(project, packageDir, password, taskId);
                    addLog(taskId, "\u6253\u5305\u52a0\u5bc6ZIP... OK", "INFO");

                    updateTaskProgress(taskId, 90, "\u4e0a\u4f20ZIP\u5230\u5b58\u50a8...");
                    addLog(taskId, "\u4e0a\u4f20ZIP\u5230\u5b58\u50a8...", "INFO");
                    uploadZipToMinio(taskId, projectId, zipPath);
                    addLog(taskId, "\u4e0a\u4f20ZIP\u5230\u5b58\u50a8... OK", "INFO");

                    updateTaskStatus(taskId, PackageStatus.SUCCESS, 100, "\u6253\u5305\u5b8c\u6210");
                    addLog(taskId, "\u6253\u5305\u5b8c\u6210!", "INFO");

                    savePackageRecord(taskId, projectId, zipPath, true, PackageStatus.SUCCESS);

                } finally {
                    try {
                        deleteDirectory(packageDir);
                    } catch (Exception e) {
                        log.warn("Failed to clean temp directory: {}", packageDir, e);
                    }
                    if (zipPath != null) {
                        try { Files.deleteIfExists(zipPath); } catch (Exception e) { log.warn("Failed to cleanup zip: {}", zipPath, e); }
                    }
                }

            } catch (Exception e) {
                log.error("Package task failed: taskId={}, projectId={}", taskId, projectId, e);
                updateTaskStatus(taskId, PackageStatus.FAILED, 0, "\u6253\u5305\u5931\u8d25: " + e.getMessage());
                addLog(taskId, "\u6253\u5305\u5931\u8d25: " + e.getMessage(), "ERROR");
                savePackageRecord(taskId, projectId, null, false, PackageStatus.FAILED);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Package task interrupted: {}", taskId, e);
            updateTaskStatus(taskId, PackageStatus.FAILED, 0, "\u6253\u5305\u4efb\u52a1\u88ab\u4e2d\u65ad");
            addLog(taskId, "\u6253\u5305\u4efb\u52a1\u88ab\u4e2d\u65ad", "ERROR");
            savePackageRecord(taskId, projectId, null, false, PackageStatus.FAILED);
        } finally {
            packageSemaphore.release();
            activeProjectTasks.remove(projectId);
            taskLogs.remove(taskId);
            try {
                PackageTask clearTask = packageTaskMapper.selectById(taskId);
                if (clearTask != null) {
                    clearTask.setPassword(null);
                    packageTaskMapper.updateById(clearTask);
                }
            } catch (Exception e) {
                log.warn("Failed to clear password for task: {}", taskId);
            }
        }
    }

    /**
     * Inject license files into the package directory.
     */
    private void injectLicense(Long licenseId, Path packageDir) throws Exception {
        LicenseRecord record = licenseRecordMapper.selectById(licenseId);
        if (record == null) {
            throw new RuntimeException("License\u8bb0\u5f55\u4e0d\u5b58\u5728: " + licenseId);
        }

        Path licenseDir = packageDir.resolve("license");
        Files.createDirectories(licenseDir);
        Path licPath = licenseDir.resolve("license.lic");
        try (InputStream is = minioClient.getObject(GetObjectArgs.builder()
                .bucket(record.getMinioBucket())
                .object(record.getMinioLicObjectKey())
                .build())) {
            Files.copy(is, licPath, StandardCopyOption.REPLACE_EXISTING);
        }
        log.info("License file injected: {}", licPath);

        Path runtimeDir = packageDir.resolve(".runtime");
        Files.createDirectories(runtimeDir);
        Path timestampPath = runtimeDir.resolve("timestamp.dat");
        try (InputStream is = minioClient.getObject(GetObjectArgs.builder()
                .bucket(record.getMinioBucket())
                .object(record.getMinioTimestampObjectKey())
                .build())) {
            Files.copy(is, timestampPath, StandardCopyOption.REPLACE_EXISTING);
        }
        log.info("Timestamp file injected: {}", timestampPath);
    }

    public PackageProgressDTO getProgress(String taskId) {
        PackageTask task = packageTaskMapper.selectById(taskId);
        PackageProgressDTO dto = new PackageProgressDTO();
        if (task == null) {
            dto.setTaskId(taskId);
            dto.setStatus(PackageStatus.FAILED.getCode());
            dto.setProgress(0);
            dto.setCurrentStep("\u4efb\u52a1\u4e0d\u5b58\u5728");
            return dto;
        }

        dto.setTaskId(taskId);
        dto.setStatus(task.getStatus());
        dto.setProgress(task.getProgress());
        dto.setCurrentStep(task.getCurrentStep());
        dto.setLogs(taskLogs.getOrDefault(taskId, new ArrayList<>()));
        return dto;
    }

    public PageResult<PackageRecord> getPackageRecords(Long projectId, String status, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<PackageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PackageRecord::getProjectId, projectId)
                .eq(PackageRecord::getDeleted, false);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(PackageRecord::getStatus, status);
        }
        wrapper.orderByDesc(PackageRecord::getCreatedAt);

        long total = packageRecordMapper.selectCount(wrapper);
        int offset = (pageNum - 1) * pageSize;
        wrapper.last("LIMIT " + offset + ", " + pageSize);
        List<PackageRecord> records = packageRecordMapper.selectList(wrapper);

        return PageResult.of(records, total, pageNum, pageSize);
    }

    public PackageRecord getLatestSuccessPackage(Long projectId) {
        LambdaQueryWrapper<PackageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PackageRecord::getProjectId, projectId)
                .eq(PackageRecord::getStatus, "SUCCESS")
                .eq(PackageRecord::getDeleted, false)
                .orderByDesc(PackageRecord::getCreatedAt)
                .last("LIMIT 1");
        return packageRecordMapper.selectOne(wrapper);
    }

    public InputStream downloadPackage(Long id) throws Exception {
        PackageRecord record = packageRecordMapper.selectById(id);
        if (record == null) {
            throw new RuntimeException("\u6253\u5305\u8bb0\u5f55\u4e0d\u5b58\u5728");
        }
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(record.getMinioBucket())
                .object(record.getMinioObjectKey())
                .build());
    }

    public PackageRecord getPackageRecord(Long id) {
        return packageRecordMapper.selectById(id);
    }

    @Transactional
    public void deletePackage(Long id) throws Exception {
        PackageRecord record = packageRecordMapper.selectById(id);
        if (record != null) {
            try {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(record.getMinioBucket())
                        .object(record.getMinioObjectKey())
                        .build());
            } catch (Exception e) {
                log.warn("Failed to delete package from MinIO: {}", record.getMinioObjectKey(), e);
            }
            packageRecordMapper.deleteById(id);
        }
    }

    private void collectJar(Project project, Path packageDir) throws Exception {
        UploadedFile jarFile = uploadedFileMapper.selectById(project.getJarFileId());
        if (jarFile == null) {
            throw new RuntimeException("JAR\u5305\u6587\u4ef6\u4e0d\u5b58\u5728");
        }
        Path appDir = packageDir.resolve("app");
        Files.createDirectories(appDir);

        downloadFromMinio(jarFile.getMinioBucket(), jarFile.getMinioObjectKey(),
                appDir.resolve(jarFile.getFileName()));
    }

    private void collectVue(Project project, Path packageDir) throws Exception {
        if (project.getVueFolderId() == null) {
            return;
        }
        UploadedFile vueFolder = uploadedFileMapper.selectById(project.getVueFolderId());
        if (vueFolder == null) {
            return;
        }

        Path htmlDir = packageDir.resolve("nginx").resolve("html");
        Files.createDirectories(htmlDir);

        String prefix = vueFolder.getMinioObjectKey() + "/";
        try {
            Iterable<Result<Item>> items = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(vueFolder.getMinioBucket())
                    .prefix(prefix)
                    .recursive(true)
                    .build());

            for (Result<Item> itemResult : items) {
                Item item = itemResult.get();
                if (item.isDir()) continue;

                String objectName = item.objectName();
                String relativePath = objectName.length() > prefix.length() ? objectName.substring(prefix.length()) : "";
                Path targetPath = htmlDir.resolve(relativePath);
                Files.createDirectories(targetPath.getParent());
                downloadFromMinio(vueFolder.getMinioBucket(), item.objectName(), targetPath);
            }
        } catch (Exception e) {
            log.warn("Failed to list Vue folder from MinIO", e);
            throw new RuntimeException("Vue\u524d\u7aef\u6587\u4ef6\u5939\u5217\u4e3e\u5931\u8d25: " + e.getMessage(), e);
        }
    }

    private void collectNginxConf(Project project, Path packageDir) throws Exception {
        Path confDir = packageDir.resolve("nginx").resolve("conf");
        Files.createDirectories(confDir);
        Path confPath = confDir.resolve("nginx.conf");

        if (project.getNginxConfContent() != null && !project.getNginxConfContent().trim().isEmpty()) {
            Files.write(confPath, project.getNginxConfContent().getBytes(StandardCharsets.UTF_8));
            return;
        }

        if (project.getNginxConfFileId() != null) {
            UploadedFile confFile = uploadedFileMapper.selectById(project.getNginxConfFileId());
            if (confFile != null) {
                downloadFromMinio(confFile.getMinioBucket(), confFile.getMinioObjectKey(), confPath);
                return;
            }
        }
    }

    private void generateAppConfig(Project project, Path packageDir) throws Exception {
        Path configDir = packageDir.resolve("app").resolve("config");
        Files.createDirectories(configDir);

        StringBuilder sb = new StringBuilder();
        sb.append("server:\n");
        sb.append("  port: ").append(project.getAppPort()).append("\n");

        if (Boolean.TRUE.equals(project.getIncludeMysql())) {
            sb.append("\n");
            sb.append("spring:\n");
            sb.append("  datasource:\n");
            String dbUrl = "jdbc:mysql://localhost:" + (project.getMysqlPort() != null ? project.getMysqlPort() : 3306)
                    + "/" + (project.getDbName() != null ? project.getDbName() : "")
                    + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai";
            sb.append("    url: ").append(dbUrl).append("\n");
            if ("clean".equals(project.getMysqlConfigState())) {
                sb.append("    username: ").append(project.getDbUsername() != null ? project.getDbUsername() : "").append("\n");
                sb.append("    password: ").append(project.getDbPassword() != null ? project.getDbPassword() : "").append("\n");
            }
        }

        if (Boolean.TRUE.equals(project.getIncludeMinio())) {
            sb.append("\n");
            sb.append("minio:\n");
            sb.append("  endpoint: http://localhost:").append(project.getMinioApiPort() != null ? project.getMinioApiPort() : 9000).append("\n");
            if ("clean".equals(project.getMinioConfigState())) {
                sb.append("  access-key: ").append(project.getMinioAccessKey() != null ? project.getMinioAccessKey() : "").append("\n");
                sb.append("  secret-key: ").append(project.getMinioSecretKey() != null ? project.getMinioSecretKey() : "").append("\n");
            }
        }

        Files.write(configDir.resolve("application.yml"), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void collectInfraComponentById(Long componentId, Path targetDir, String label) throws Exception {
        if (componentId == null) {
            throw new RuntimeException(label + "\u7ec4\u4ef6\u672a\u9009\u62e9\uff0c\u8bf7\u5148\u5728\u9879\u76ee\u914d\u7f6e\u4e2d\u9009\u62e9" + label + "\u7248\u672c");
        }
        UploadedFile component = uploadedFileMapper.selectById(componentId);
        if (component == null) {
            throw new RuntimeException(label + "\u7ec4\u4ef6\u4e0d\u5b58\u5728(ID=" + componentId + ")\uff0c\u8bf7\u91cd\u65b0\u9009\u62e9");
        }

        Files.createDirectories(targetDir);

        if (Boolean.TRUE.equals(component.getIsFolder())) {
            String prefix = component.getMinioObjectKey() + "/";
            try {
                Iterable<Result<Item>> items = minioClient.listObjects(ListObjectsArgs.builder()
                        .bucket(component.getMinioBucket())
                        .prefix(prefix)
                        .recursive(true)
                        .build());

                for (Result<Item> itemResult : items) {
                    Item item = itemResult.get();
                    if (item.isDir()) continue;

                    String objectName = item.objectName();
                    String relativePath = objectName.length() > prefix.length() ? objectName.substring(prefix.length()) : "";
                    Path targetPath = targetDir.resolve(relativePath);
                    Files.createDirectories(targetPath.getParent());
                    downloadFromMinio(component.getMinioBucket(), item.objectName(), targetPath);
                }
            } catch (Exception e) {
                throw new RuntimeException(label + "\u7ec4\u4ef6\u4e0b\u8f7d\u5931\u8d25: " + e.getMessage(), e);
            }
        } else {
            downloadFromMinio(component.getMinioBucket(), component.getMinioObjectKey(),
                    targetDir.resolve(component.getFileName()));
        }
    }

    private void downloadFromMinio(String bucket, String objectKey, Path targetPath) throws Exception {
        try (InputStream is = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .build())) {
            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path createEncryptedZip(Project project, Path sourceDir, String password, String taskId) throws Exception {
        String zipFileName = project.getName() + "-" + taskId + "-release.zip";
        Path zipPath = Paths.get(tempDir, zipFileName);

        char[] passwordChars = password.toCharArray();
        ZipParameters zipParams = new ZipParameters();
        zipParams.setEncryptFiles(true);
        zipParams.setEncryptionMethod(EncryptionMethod.AES);
        zipParams.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);

        try (ZipFile zipFile = new ZipFile(zipPath.toFile(), passwordChars)) {
            Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws java.io.IOException {
                    Path relativePath = sourceDir.relativize(file);
                    ZipParameters params = new ZipParameters();
                    params.setEncryptFiles(true);
                    params.setEncryptionMethod(EncryptionMethod.AES);
                    params.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
                    params.setFileNameInZip(relativePath.toString().replace("\\", "/"));
                    try {
                        zipFile.addFile(file.toFile(), params);
                    } catch (net.lingala.zip4j.exception.ZipException e) {
                        throw new RuntimeException(e);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        return zipPath;
    }

    private void uploadZipToMinio(String taskId, Long projectId, Path zipPath) throws Exception {
        String bucket = minioConfig.getBucketPackages();
        String objectKey = projectId + "/" + taskId + "/" + zipPath.getFileName().toString();

        ensureBucketExists(bucket);

        minioClient.uploadObject(UploadObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .filename(zipPath.toString())
                .build());
    }

    public void savePackageRecord(String taskId, Long projectId, Path zipPath, boolean encrypted, PackageStatus status) {
        PackageRecord record = new PackageRecord();
        record.setProjectId(projectId);
        record.setTaskId(taskId);
        record.setStatus(status.getCode());
        record.setEncrypted(encrypted);

        if (zipPath != null) {
            record.setFileName(zipPath.getFileName().toString());
            try {
                record.setFileSize(Files.size(zipPath));
            } catch (Exception e) {
                record.setFileSize(0L);
            }
            record.setMinioBucket(minioConfig.getBucketPackages());
            record.setMinioObjectKey(projectId + "/" + taskId + "/" + zipPath.getFileName().toString());
        } else {
            record.setFileName("failed-" + taskId + ".zip");
            record.setFileSize(0L);
        }

        packageRecordMapper.insert(record);
    }

    private void updateTaskStatus(String taskId, PackageStatus status, int progress, String currentStep) {
        PackageTask task = packageTaskMapper.selectById(taskId);
        if (task != null) {
            task.setStatus(status.getCode());
            task.setProgress(progress);
            task.setCurrentStep(currentStep);
            if (status == PackageStatus.SUCCESS || status == PackageStatus.FAILED) {
                task.setFinishedAt(LocalDateTime.now());
            }
            packageTaskMapper.updateById(task);
        }
    }

    private void updateTaskProgress(String taskId, int progress, String currentStep) {
        PackageTask task = packageTaskMapper.selectById(taskId);
        if (task != null) {
            task.setProgress(progress);
            task.setCurrentStep(currentStep);
            packageTaskMapper.updateById(task);
        }
    }

    private void addLog(String taskId, String message, String level) {
        List<PackageProgressDTO.LogEntry> logs = taskLogs.computeIfAbsent(taskId, k -> new ArrayList<>());
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        logs.add(new PackageProgressDTO.LogEntry(time, message, level));
        if (logs.size() > 500) {
            logs.subList(0, logs.size() - 500).clear();
        }
    }

    private void deleteDirectory(Path dir) throws Exception {
        if (!Files.exists(dir)) return;
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws java.io.IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path d, java.io.IOException exc) throws java.io.IOException {
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void ensureBucketExists(String bucket) throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("Bucket created: {}", bucket);
        }
    }
}
