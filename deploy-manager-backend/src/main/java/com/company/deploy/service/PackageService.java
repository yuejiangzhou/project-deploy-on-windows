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

    /**
     * 注意：此方法不能加 @Transactional。
     * 原因：@Async executePackageAsync 在新线程/新事务中执行，若本方法带事务，
     * task(PENDING) 的插入在方法返回后才提交，异步线程此时 selectById 查不到 task，
     * 导致 FAILED/SUCCESS 状态无法更新，前端永远停留在"打包中"。
     * 单条 insert 无需事务包裹，移除 @Transactional 让插入立即提交，异步线程即可见。
     */
    public String startPackage(Long projectId, String password) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }

        if (!"READY".equals(project.getStatus())) {
            throw new RuntimeException("项目状态不是 READY，无法打包");
        }

        // 并发控制：同一项目不允许同时有多个打包任务
        if (activeProjectTasks.containsKey(projectId)) {
            throw new RuntimeException("该项目已有打包任务在进行中，请等待完成后再试");
        }

        // 检查数据库中是否有未完成的任务（防止重启后状态不一致）
        LambdaQueryWrapper<PackageTask> activeCheck = new LambdaQueryWrapper<>();
        activeCheck.eq(PackageTask::getProjectId, projectId)
                .in(PackageTask::getStatus, PackageStatus.PENDING.getCode(), PackageStatus.RUNNING.getCode());
        Long activeCount = packageTaskMapper.selectCount(activeCheck);
        if (activeCount != null && activeCount > 0) {
            throw new RuntimeException("该项目已有打包任务在进行中，请等待完成后再试");
        }

        String taskId = UUID.randomUUID().toString();

        PackageTask task = new PackageTask();
        task.setId(taskId);
        task.setProjectId(projectId);
        task.setStatus(PackageStatus.PENDING.getCode());
        task.setProgress(0);
        task.setCurrentStep("等待打包...");
        task.setPassword(password);
        task.setCreatedAt(LocalDateTime.now());
        packageTaskMapper.insert(task);

        activeProjectTasks.put(projectId, taskId);
        taskLogs.put(taskId, new ArrayList<>());
        addLog(taskId, "开始打包 " + project.getName() + "...", "INFO");

        self.executePackageAsync(taskId, projectId, password);

        return taskId;
    }

    @Async("packageTaskExecutor")
    public void executePackageAsync(String taskId, Long projectId, String password) {
        try {
            addLog(taskId, "排队等待打包资源...", "INFO");
            packageSemaphore.acquire();

            try {
                updateTaskStatus(taskId, PackageStatus.RUNNING, 0, "验证配置完整性...");
                addLog(taskId, "验证配置完整性...", "INFO");

                Project project = projectMapper.selectById(projectId);
                if (project == null) {
                    throw new RuntimeException("项目不存在");
                }

                if (project.getJarFileId() == null) {
                    throw new RuntimeException("请先配置JAR包");
                }

                Path packageDir = Paths.get(tempDir, taskId);
                Files.createDirectories(packageDir);
                Path zipPath = null;

                try {
                    updateTaskProgress(taskId, 5, "收集后端JAR包...");
                    addLog(taskId, "收集后端JAR包...", "INFO");
                    collectJar(project, packageDir);
                    addLog(taskId, "收集后端JAR包... OK", "INFO");

                    updateTaskProgress(taskId, 15, "收集JDK环境...");
                    addLog(taskId, "收集JDK环境...", "INFO");
                    collectInfraComponentById(project.getJdkComponentId(), packageDir.resolve("jdk"), "JDK");
                    addLog(taskId, "收集JDK环境... OK", "INFO");

                    updateTaskProgress(taskId, 30, "收集Vue前端 + Nginx...");
                    addLog(taskId, "收集Vue前端 + Nginx...", "INFO");
                    collectVue(project, packageDir);
                    if (Boolean.TRUE.equals(project.getIncludeNginx())) {
                        collectInfraComponentById(project.getNginxComponentId(), packageDir.resolve("nginx"), "Nginx");
                        collectNginxConf(project, packageDir);
                    }
                    addLog(taskId, "收集Vue前端 + Nginx... OK", "INFO");

                    if (Boolean.TRUE.equals(project.getIncludeMysql())) {
                        updateTaskProgress(taskId, 45, "收集MySQL便携包...");
                        addLog(taskId, "收集MySQL便携包...", "INFO");
                        collectInfraComponentById(project.getMysqlComponentId(), packageDir.resolve("mysql"), "MySQL");
                        addLog(taskId, "收集MySQL便携包... OK", "INFO");
                    }

                    if (Boolean.TRUE.equals(project.getIncludeMinio())) {
                        updateTaskProgress(taskId, 55, "收集MinIO...");
                        addLog(taskId, "收集MinIO...", "INFO");
                        collectInfraComponentById(project.getMinioComponentId(), packageDir.resolve("minio"), "MinIO");
                        addLog(taskId, "收集MinIO... OK", "INFO");
                    }

                    if (Boolean.TRUE.equals(project.getEngineEnabled())) {
                        updateTaskProgress(taskId, 62, "收集引擎...");
                        addLog(taskId, "收集引擎...", "INFO");
                        collectInfraComponentById(project.getEngineComponentId(), packageDir.resolve("engine"), "引擎");
                        addLog(taskId, "收集引擎... OK", "INFO");
                    }

                    updateTaskProgress(taskId, 70, "生成启动脚本和应用配置...");
                    addLog(taskId, "生成启动脚本和应用配置...", "INFO");
                    scriptTemplateService.generateScripts(project, packageDir);
                    generateAppConfig(project, packageDir);
                    addLog(taskId, "生成启动脚本和应用配置... OK", "INFO");

                    updateTaskProgress(taskId, 80, "正在打包加密ZIP...");
                    addLog(taskId, "正在打包加密ZIP...", "INFO");
                    zipPath = createEncryptedZip(project, packageDir, password, taskId);
                    addLog(taskId, "打包加密ZIP... OK", "INFO");

                    updateTaskProgress(taskId, 90, "上传ZIP到存储...");
                    addLog(taskId, "上传ZIP到存储...", "INFO");
                    uploadZipToMinio(taskId, projectId, zipPath);
                    addLog(taskId, "上传ZIP到存储... OK", "INFO");

                    updateTaskStatus(taskId, PackageStatus.SUCCESS, 100, "打包完成");
                    addLog(taskId, "打包完成!", "INFO");

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
                    // 不移除 taskLogs，让外层的 finally 或后续查询仍可访问
                    // 异常时日志会被保留，FAILED 状态也能看到失败前的日志
                }

            } catch (Exception e) {
                log.error("Package task failed: taskId={}, projectId={}", taskId, projectId, e);
                updateTaskStatus(taskId, PackageStatus.FAILED, 0, "打包失败: " + e.getMessage());
                addLog(taskId, "打包失败: " + e.getMessage(), "ERROR");
                savePackageRecord(taskId, projectId, null, false, PackageStatus.FAILED);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Package task interrupted: {}", taskId, e);
            updateTaskStatus(taskId, PackageStatus.FAILED, 0, "打包任务被中断");
            addLog(taskId, "打包任务被中断", "ERROR");
            savePackageRecord(taskId, projectId, null, false, PackageStatus.FAILED);
        } finally {
            packageSemaphore.release();
            activeProjectTasks.remove(projectId);
            taskLogs.remove(taskId);
            // Clear password after task completion
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

    public PackageProgressDTO getProgress(String taskId) {
        PackageTask task = packageTaskMapper.selectById(taskId);
        PackageProgressDTO dto = new PackageProgressDTO();
        if (task == null) {
            dto.setTaskId(taskId);
            dto.setStatus(PackageStatus.FAILED.getCode());
            dto.setProgress(0);
            dto.setCurrentStep("任务不存在");
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
            throw new RuntimeException("打包记录不存在");
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
            throw new RuntimeException("JAR包文件不存在");
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
            throw new RuntimeException("Vue前端文件夹列举失败: " + e.getMessage(), e);
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
            throw new RuntimeException(label + "组件未选择，请先在项目配置中选择" + label + "版本");
        }
        UploadedFile component = uploadedFileMapper.selectById(componentId);
        if (component == null) {
            throw new RuntimeException(label + "组件不存在(ID=" + componentId + ")，请重新选择");
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
                throw new RuntimeException(label + "组件下载失败: " + e.getMessage(), e);
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
