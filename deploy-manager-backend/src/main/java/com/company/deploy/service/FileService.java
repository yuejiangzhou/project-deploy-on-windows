package com.company.deploy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.deploy.config.MinioConfig;
import com.company.deploy.dto.FileTreeNode;
import com.company.deploy.entity.UploadedFile;
import com.company.deploy.enums.FileTypeEnum;
import com.company.deploy.mapper.UploadedFileMapper;
import io.minio.*;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final UploadedFileMapper uploadedFileMapper;

    public List<FileTreeNode> getFileTree(Long projectId) {
        LambdaQueryWrapper<UploadedFile> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(UploadedFile::getProjectId, projectId);
        } else {
            wrapper.isNotNull(UploadedFile::getProjectId);
        }
        wrapper.eq(UploadedFile::getDeleted, false)
                .orderByDesc(UploadedFile::getUploadDate);

        List<UploadedFile> files = uploadedFileMapper.selectList(wrapper);

        Map<String, FileTreeNode> projectNodes = new LinkedHashMap<>();

        for (UploadedFile file : files) {
            if (file.getProjectId() == null) continue;

            String projectKey = String.valueOf(file.getProjectId());
            FileTreeNode projectNode = projectNodes.computeIfAbsent(projectKey,
                    k -> new FileTreeNode("project-" + k, "项目" + k, "project", k));

            String typeName = FileTypeEnum.of(file.getFileType()) != null
                    ? FileTypeEnum.of(file.getFileType()).getDesc() : file.getFileType();
            String typeKey = file.getFileType();

            FileTreeNode typeNode = null;
            for (FileTreeNode child : projectNode.getChildren()) {
                if (child.getId().equals("type-" + typeKey)) {
                    typeNode = child;
                    break;
                }
            }
            if (typeNode == null) {
                typeNode = new FileTreeNode("type-" + typeKey, typeName, "type",
                        projectKey + "/" + typeKey);
                projectNode.getChildren().add(typeNode);
            }

            if (file.getUploadDate() != null) {
                String dateStr = file.getUploadDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
                String dateKey = typeKey + "-" + dateStr;
                FileTreeNode dateNode = null;
                for (FileTreeNode child : typeNode.getChildren()) {
                    if (child.getId().equals("date-" + dateKey)) {
                        dateNode = child;
                        break;
                    }
                }
                if (dateNode == null) {
                    dateNode = new FileTreeNode("date-" + dateKey, dateStr, "date",
                            projectKey + "/" + typeKey + "/" + dateStr);
                    typeNode.getChildren().add(dateNode);
                }
            }
        }

        return new ArrayList<>(projectNodes.values());
    }

    public List<UploadedFile> listFiles(Long projectId, String type, String date) {
        LambdaQueryWrapper<UploadedFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UploadedFile::getProjectId, projectId)
                .eq(UploadedFile::getFileType, type)
                .eq(UploadedFile::getDeleted, false);
        if (date != null && !date.isEmpty()) {
            wrapper.eq(UploadedFile::getUploadDate, LocalDate.parse(date));
        }
        wrapper.orderByDesc(UploadedFile::getCreatedAt);
        return uploadedFileMapper.selectList(wrapper);
    }

    public List<UploadedFile> getPathOptions(Long projectId, String type) {
        LambdaQueryWrapper<UploadedFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UploadedFile::getProjectId, projectId)
                .eq(UploadedFile::getFileType, type)
                .eq(UploadedFile::getDeleted, false)
                .orderByDesc(UploadedFile::getUploadDate, UploadedFile::getCreatedAt);
        return uploadedFileMapper.selectList(wrapper);
    }

    @Transactional
    public UploadedFile uploadFile(MultipartFile file, Long projectId, String type, String date, String versionTag)
            throws IOException, ServerException, InsufficientDataException, ErrorResponseException,
            NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        String bucket = minioConfig.getBucketProjectFiles();
        ensureBucketExists(bucket);

        LocalDate uploadDate = (date != null && !date.isEmpty())
                ? LocalDate.parse(date) : LocalDate.now();
        String dateStr = uploadDate.format(DateTimeFormatter.ISO_LOCAL_DATE);

        String safeName = sanitizeFileName(file.getOriginalFilename());
        String objectKey = projectId + "/" + type + "/" + dateStr + "/" + safeName;

        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(is, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        }

        UploadedFile uploadedFile = new UploadedFile();
        uploadedFile.setProjectId(projectId);
        uploadedFile.setFileType(type);
        uploadedFile.setFileName(safeName);
        uploadedFile.setFileSize(file.getSize());
        uploadedFile.setMinioBucket(bucket);
        uploadedFile.setMinioObjectKey(objectKey);
        uploadedFile.setIsFolder(false);
        uploadedFile.setFolderPath(projectId + "/" + type + "/" + dateStr);
        uploadedFile.setUploadDate(uploadDate);
        uploadedFile.setVersionTag(versionTag);
        uploadedFileMapper.insert(uploadedFile);

        log.info("File uploaded: {} -> {}", safeName, objectKey);
        return uploadedFile;
    }

    @Transactional
    public UploadedFile uploadFolder(List<MultipartFile> files, Long projectId, String type,
                                     String date, String folderName, String versionTag)
            throws IOException, ServerException, InsufficientDataException, ErrorResponseException,
            NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        String bucket = minioConfig.getBucketProjectFiles();
        ensureBucketExists(bucket);

        LocalDate uploadDate = (date != null && !date.isEmpty())
                ? LocalDate.parse(date) : LocalDate.now();
        String dateStr = uploadDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String basePath = projectId + "/" + type + "/" + dateStr + "/" + folderName;

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

        UploadedFile folderFile = new UploadedFile();
        folderFile.setProjectId(projectId);
        folderFile.setFileType(type);
        folderFile.setFileName(folderName);
        folderFile.setFileSize(totalSize);
        folderFile.setMinioBucket(bucket);
        folderFile.setMinioObjectKey(basePath);
        folderFile.setIsFolder(true);
        folderFile.setFolderPath(projectId + "/" + type + "/" + dateStr);
        folderFile.setUploadDate(uploadDate);
        folderFile.setVersionTag(versionTag);
        uploadedFileMapper.insert(folderFile);

        log.info("Folder uploaded: {} -> {} ({} files, {} bytes)", folderName, basePath, files.size(), totalSize);
        return folderFile;
    }

    @Transactional
    public void deleteFile(Long id) throws ServerException, InsufficientDataException,
            ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException,
            InvalidResponseException, XmlParserException, InternalException {
        UploadedFile file = uploadedFileMapper.selectById(id);
        if (file != null) {
            try {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(file.getMinioBucket())
                        .object(file.getMinioObjectKey())
                        .build());
            } catch (Exception e) {
                log.warn("Failed to delete object from MinIO: {}", file.getMinioObjectKey(), e);
            }
            uploadedFileMapper.deleteById(id);
        }
    }

    public InputStream downloadFile(Long id) throws ServerException, InsufficientDataException,
            ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException,
            InvalidResponseException, XmlParserException, InternalException {
        UploadedFile file = uploadedFileMapper.selectById(id);
        if (file == null) {
            throw new RuntimeException("File not found");
        }
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(file.getMinioBucket())
                .object(file.getMinioObjectKey())
                .build());
    }

    public UploadedFile getFileById(Long id) {
        return uploadedFileMapper.selectById(id);
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

    private void ensureBucketExists(String bucket) throws ServerException, InsufficientDataException,
            ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException,
            InvalidResponseException, XmlParserException, InternalException {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("Bucket created: {}", bucket);
        }
    }
}
