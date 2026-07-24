package com.company.deploy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.deploy.config.MinioConfig;
import com.company.deploy.entity.UploadedFile;
import com.company.deploy.enums.FileTypeEnum;
import com.company.deploy.mapper.UploadedFileMapper;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InfrastructureService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final UploadedFileMapper uploadedFileMapper;

    public List<UploadedFile> listComponents(String type) {
        LambdaQueryWrapper<UploadedFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(UploadedFile::getProjectId)
                .eq(UploadedFile::getFileType, type)
                .eq(UploadedFile::getDeleted, false)
                .orderByDesc(UploadedFile::getCreatedAt);
        return uploadedFileMapper.selectList(wrapper);
    }

    @Transactional
    public UploadedFile uploadComponent(String type, MultipartFile file, String version, String tag,
                                        String initState, String belongsTo) throws Exception {
        String bucket = minioConfig.getBucketInfraComponents();
        ensureBucketExists(bucket);

        String safeName = sanitizeFileName(file.getOriginalFilename());
        String versionPath = (version != null && !version.trim().isEmpty()) ? version : "unversioned";
        String objectKey = type + "/" + versionPath + "/" + System.currentTimeMillis() + "-" + safeName;

        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(is, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        }

        UploadedFile uploadedFile = new UploadedFile();
        uploadedFile.setProjectId(null);
        uploadedFile.setFileType(type);
        uploadedFile.setFileName(safeName);
        uploadedFile.setFileSize(file.getSize());
        uploadedFile.setMinioBucket(bucket);
        uploadedFile.setMinioObjectKey(objectKey);
        uploadedFile.setIsFolder(false);
        uploadedFile.setVersion(version);
        uploadedFile.setTag(tag);
        uploadedFile.setInitState(initState);
        uploadedFile.setBelongsTo(belongsTo);
        uploadedFileMapper.insert(uploadedFile);

        log.info("Infrastructure component uploaded: {} -> {}", file.getOriginalFilename(), objectKey);
        return uploadedFile;
    }

    @Transactional
    public UploadedFile uploadComponentFolder(String type, List<MultipartFile> files, String folderName,
                                              String version, String tag, String initState, String belongsTo) throws Exception {
        String bucket = minioConfig.getBucketInfraComponents();
        ensureBucketExists(bucket);

        String basePath = type + "/" + folderName;
        long totalSize = 0;
        List<String> uploadedObjects = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                String relativePath = sanitizeFileName(file.getOriginalFilename());
                String objectKey = basePath + "/" + relativePath;

                try (InputStream is = file.getInputStream()) {
                    minioClient.putObject(PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(is, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
                }
                uploadedObjects.add(objectKey);
                totalSize += file.getSize();
            }
        } catch (Exception e) {
            // Compensate: remove already uploaded objects
            for (String objKey : uploadedObjects) {
                try {
                    minioClient.removeObject(RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objKey)
                            .build());
                } catch (Exception ignored) {
                    log.warn("Failed to cleanup MinIO object: {}", objKey);
                }
            }
            throw e;
        }

        UploadedFile folder = new UploadedFile();
        folder.setProjectId(null);
        folder.setFileType(type);
        folder.setFileName(folderName);
        folder.setFileSize(totalSize);
        folder.setMinioBucket(bucket);
        folder.setMinioObjectKey(basePath);
        folder.setIsFolder(true);
        folder.setVersion(version);
        folder.setTag(tag);
        folder.setInitState(initState);
        folder.setBelongsTo(belongsTo);
        uploadedFileMapper.insert(folder);

        log.info("Infrastructure folder uploaded: {} -> {} ({} files)", folderName, basePath, files.size());
        return folder;
    }

    @Transactional
    public UploadedFile uploadComponentZip(String type, MultipartFile zipFile, String version, String tag,
                                           String initState, String belongsTo) throws Exception {
        String bucket = minioConfig.getBucketInfraComponents();
        ensureBucketExists(bucket);

        String originalName = zipFile.getOriginalFilename();
        if (originalName == null) {
            throw new RuntimeException("文件名不能为空");
        }
        String lowerName = originalName.toLowerCase();

        java.io.File tempFile = java.io.File.createTempFile("upload-", lowerName.substring(lowerName.lastIndexOf(".")));
        zipFile.transferTo(tempFile);

        String folderName;
        if (lowerName.endsWith(".zip")) {
            folderName = originalName.substring(0, originalName.length() - 4);
        } else if (lowerName.endsWith(".tar.gz") || lowerName.endsWith(".tgz")) {
            folderName = originalName.substring(0, lowerName.endsWith(".tar.gz") ? originalName.length() - 7 : originalName.length() - 4);
        } else if (lowerName.endsWith(".tar")) {
            folderName = originalName.substring(0, originalName.length() - 4);
        } else if (lowerName.endsWith(".7z")) {
            folderName = originalName.substring(0, originalName.length() - 3);
        } else {
            throw new RuntimeException("不支持的压缩格式，请上传 zip/tar/tar.gz/7z 格式文件");
        }

        String versionPath = (version != null && !version.trim().isEmpty()) ? version : "unversioned";
        String basePath = type + "/" + versionPath + "/" + folderName;

        long totalSize = 0;
        int fileCount = 0;
        List<String> uploadedObjects = new ArrayList<>();

        try {
            if (lowerName.endsWith(".zip")) {
                ZipFile zip = new ZipFile(tempFile);
                try {
                    @SuppressWarnings("unchecked")
                    List<FileHeader> headers = zip.getFileHeaders();
                    for (FileHeader header : headers) {
                        if (header.isDirectory()) continue;
                        String relativePath = header.getFileName();
                        if (relativePath == null || relativePath.isEmpty()) continue;

                        String objectKey = basePath + "/" + relativePath;
                        InputStream is = zip.getInputStream(header);
                        long size = header.getUncompressedSize();
                        try {
                            minioClient.putObject(PutObjectArgs.builder()
                                    .bucket(bucket)
                                    .object(objectKey)
                                    .stream(is, size, -1)
                                    .build());
                        } finally {
                            is.close();
                        }
                        uploadedObjects.add(objectKey);
                        totalSize += size;
                        fileCount++;
                    }
                } finally {
                    zip.close();
                }
            } else if (lowerName.endsWith(".tar") || lowerName.endsWith(".tar.gz") || lowerName.endsWith(".tgz")) {
                try (java.io.FileInputStream fis = new java.io.FileInputStream(tempFile);
                     InputStream decompressed = lowerName.endsWith(".tar.gz") || lowerName.endsWith(".tgz")
                             ? new java.util.zip.GZIPInputStream(fis) : fis;
                     org.apache.commons.compress.archivers.tar.TarArchiveInputStream tis =
                             new org.apache.commons.compress.archivers.tar.TarArchiveInputStream(decompressed)) {
                    org.apache.commons.compress.archivers.tar.TarArchiveEntry entry;
                    while ((entry = tis.getNextTarEntry()) != null) {
                        if (entry.isDirectory()) continue;
                        String relativePath = entry.getName();
                        if (relativePath == null || relativePath.isEmpty()) continue;

                        String objectKey = basePath + "/" + relativePath;
                        // 流式上传，避免将整个 tar 条目加载到内存（防止 OOM）
                        long entrySize = entry.getSize();
                        try (InputStream entryStream = new TarEntryInputStream(tis, entrySize)) {
                            minioClient.putObject(PutObjectArgs.builder()
                                    .bucket(bucket)
                                    .object(objectKey)
                                    .stream(entryStream, entrySize, -1)
                                    .build());
                        }
                        uploadedObjects.add(objectKey);
                        totalSize += entrySize;
                        fileCount++;
                    }
                }
            } else if (lowerName.endsWith(".7z")) {
                try (org.apache.commons.compress.archivers.sevenz.SevenZFile sevenZ = new org.apache.commons.compress.archivers.sevenz.SevenZFile(tempFile)) {
                    org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry entry;
                    while ((entry = sevenZ.getNextEntry()) != null) {
                        if (entry.isDirectory()) continue;
                        String relativePath = entry.getName();
                        if (relativePath == null || relativePath.isEmpty()) continue;

                        long entrySize = entry.getSize();
                        // 7z 文件通常不会像 tar 那样有超大条目（JDK 级别），但仍加保护
                        if (entrySize > 512 * 1024 * 1024) { // 超过 512MB 拒绝
                            log.warn("7z entry too large ({}MB), skipping: {}", entrySize / 1024 / 1024, relativePath);
                            continue;
                        }
                        byte[] data = new byte[(int) entrySize];
                        int offset = 0;
                        int remaining = data.length;
                        while (remaining > 0) {
                            int read = sevenZ.read(data, offset, remaining);
                            if (read == -1) break;
                            offset += read;
                            remaining -= read;
                        }
                        String objectKey = basePath + "/" + relativePath;
                        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
                            minioClient.putObject(PutObjectArgs.builder()
                                    .bucket(bucket)
                                    .object(objectKey)
                                    .stream(bais, data.length, -1)
                                    .build());
                        }
                        uploadedObjects.add(objectKey);
                        totalSize += data.length;
                        fileCount++;
                    }
                }
            }
        } catch (Exception e) {
            for (String objKey : uploadedObjects) {
                try {
                    minioClient.removeObject(RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objKey)
                            .build());
                } catch (Exception ignored) {
                    log.warn("Failed to cleanup MinIO object: {}", objKey);
                }
            }
            throw e;
        } finally {
            if (!tempFile.delete()) {
                log.warn("Failed to delete temp file: {}", tempFile.getAbsolutePath());
            }
        }

        UploadedFile folder = new UploadedFile();
        folder.setProjectId(null);
        folder.setFileType(type);
        folder.setFileName(folderName);
        folder.setFileSize(totalSize);
        folder.setMinioBucket(bucket);
        folder.setMinioObjectKey(basePath);
        folder.setIsFolder(true);
        folder.setVersion(version);
        folder.setTag(tag);
        folder.setInitState(initState);
        folder.setBelongsTo(belongsTo);
        uploadedFileMapper.insert(folder);

        log.info("Infrastructure archive uploaded and decompressed: {} -> {} ({} files)", folderName, basePath, fileCount);
        return folder;
    }

    @Transactional
    public void deleteComponent(String type, Long id) throws Exception {
        UploadedFile file = uploadedFileMapper.selectById(id);
        if (file != null) {
            try {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(file.getMinioBucket())
                        .object(file.getMinioObjectKey())
                        .build());
            } catch (Exception e) {
                log.warn("Failed to delete infra component from MinIO: {}", file.getMinioObjectKey(), e);
            }
            uploadedFileMapper.deleteById(id);
        }
    }

    public UploadedFile getLatestComponent(String type) {
        List<UploadedFile> list = listComponents(type);
        return list.isEmpty() ? null : list.get(0);
    }

    public UploadedFile getComponent(Long id) {
        return uploadedFileMapper.selectById(id);
    }

    @Transactional
    public UploadedFile updateComponent(Long id, java.util.Map<String, String> updates) {
        UploadedFile file = uploadedFileMapper.selectById(id);
        if (file == null) {
            throw new RuntimeException("组件不存在");
        }
        if (updates.containsKey("version")) {
            file.setVersion(updates.get("version"));
        }
        if (updates.containsKey("tag")) {
            file.setTag(updates.get("tag"));
        }
        if (updates.containsKey("initState")) {
            file.setInitState(updates.get("initState"));
        }
        if (updates.containsKey("belongsTo")) {
            file.setBelongsTo(updates.get("belongsTo"));
        }
        uploadedFileMapper.updateById(file);
        log.info("Infrastructure component updated: id={}", id);
        return file;
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) return null;
        // Remove path traversal sequences
        String sanitized = fileName.replace("..", "").replace("./", "").replace("\\", "/");
        // Remove leading slashes
        while (sanitized.startsWith("/")) {
            sanitized = sanitized.substring(1);
        }
        return sanitized;
    }

    private void ensureBucketExists(String bucket) throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("Bucket created: {}", bucket);
        }
    }

    /**
     * 流式读取单个 tar 条目的 InputStream，避免将整个条目加载到内存。
     * 读取到 entrySize 字节后自动返回 -1（EOF）。
     */
    private static class TarEntryInputStream extends InputStream {
        private final InputStream source;
        private long remaining;

        TarEntryInputStream(InputStream source, long size) {
            this.source = source;
            this.remaining = size;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) return -1;
            int b = source.read();
            if (b != -1) remaining--;
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) return -1;
            int toRead = (int) Math.min(len, remaining);
            int read = source.read(b, off, toRead);
            if (read > 0) remaining -= read;
            return read;
        }

        @Override
        public void close() throws IOException {
            // 不关闭底层 TarArchiveInputStream，由外层管理
        }
    }
}
