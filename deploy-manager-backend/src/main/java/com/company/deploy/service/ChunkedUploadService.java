package com.company.deploy.service;

import com.company.deploy.config.MinioConfig;
import com.company.deploy.entity.UploadedFile;
import com.company.deploy.mapper.UploadedFileMapper;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkedUploadService {

    private final MinioConfig minioConfig;
    private final MinioClient minioClient;
    private final UploadedFileMapper uploadedFileMapper;

    @Value("${package.temp-dir:./temp/packages}")
    private String tempBaseDir;

    private String chunkDir;
    private final Map<String, ChunkUploadSession> sessions = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        chunkDir = tempBaseDir + "/chunks";
        try {
            Files.createDirectories(Paths.get(chunkDir));
        } catch (Exception e) {
            log.warn("Failed to create chunk directory: {}", chunkDir, e);
        }
        log.info("Chunked upload directory initialized: {}", chunkDir);
    }

    @PreDestroy
    public void cleanup() {
        sessions.clear();
        try {
            Files.walk(Paths.get(chunkDir), 1)
                    .filter(p -> !p.equals(Paths.get(chunkDir)))
                    .forEach(p -> {
                        try { deleteDirectory(p); } catch (Exception ignored) {}
                    });
        } catch (Exception e) {
            log.warn("Failed to cleanup chunk directory", e);
        }
    }

    public Map<String, Object> initUpload(String fileName, long fileSize, int totalChunks,
                                           Long projectId, String type, String versionTag) {
        String uploadId = UUID.randomUUID().toString();
        Path sessionDir = Paths.get(chunkDir, uploadId);
        try {
            Files.createDirectories(sessionDir);
        } catch (Exception e) {
            throw new RuntimeException("创建分片上传目录失败", e);
        }

        ChunkUploadSession session = new ChunkUploadSession();
        session.uploadId = uploadId;
        session.fileName = fileName;
        session.fileSize = fileSize;
        session.totalChunks = totalChunks;
        session.projectId = projectId;
        session.type = type;
        session.versionTag = versionTag;
        session.receivedChunks = new ConcurrentHashMap<>();
        session.sessionDir = sessionDir;

        sessions.put(uploadId, session);

        Map<String, Object> result = new HashMap<>();
        result.put("uploadId", uploadId);
        result.put("chunkSize", 10 * 1024 * 1024); // 10MB per chunk
        return result;
    }

    public void uploadChunk(String uploadId, int chunkIndex, MultipartFile chunkFile) throws Exception {
        ChunkUploadSession session = sessions.get(uploadId);
        if (session == null) {
            throw new RuntimeException("上传会话不存在或已过期: " + uploadId);
        }

        Path chunkPath = session.sessionDir.resolve("chunk_" + chunkIndex);
        try (InputStream is = chunkFile.getInputStream()) {
            Files.copy(is, chunkPath, StandardCopyOption.REPLACE_EXISTING);
        }

        session.receivedChunks.put(chunkIndex, true);
        log.debug("Chunk uploaded: uploadId={}, chunk={}/{}, size={}",
                uploadId, chunkIndex + 1, session.totalChunks, chunkFile.getSize());
    }

    public UploadedFile completeUpload(String uploadId) throws Exception {
        ChunkUploadSession session = sessions.get(uploadId);
        if (session == null) {
            throw new RuntimeException("上传会话不存在或已过期: " + uploadId);
        }

        if (session.receivedChunks.size() != session.totalChunks) {
            throw new RuntimeException("分片未上传完整: 已接收 " + session.receivedChunks.size()
                    + "/" + session.totalChunks);
        }

        String bucket = minioConfig.getBucketProjectFiles();
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }

        String objectKey = session.type + "/" + session.projectId + "/" + session.fileName;

        try (InputStream is = new SequenceChunkInputStream(session.sessionDir, session.totalChunks)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(is, session.fileSize, -1)
                    .build());
        }

        UploadedFile uploadedFile = new UploadedFile();
        uploadedFile.setProjectId(session.projectId);
        uploadedFile.setFileType(session.type);
        uploadedFile.setFileName(session.fileName);
        uploadedFile.setFileSize(session.fileSize);
        uploadedFile.setMinioBucket(bucket);
        uploadedFile.setMinioObjectKey(objectKey);
        uploadedFile.setIsFolder(false);
        uploadedFile.setVersionTag(session.versionTag);
        uploadedFileMapper.insert(uploadedFile);

        log.info("Chunked upload completed: {} -> {} ({} chunks)", session.fileName, objectKey, session.totalChunks);

        sessions.remove(uploadId);
        deleteDirectory(session.sessionDir);

        return uploadedFile;
    }

    public void abortUpload(String uploadId) {
        ChunkUploadSession session = sessions.remove(uploadId);
        if (session != null) {
            try {
                deleteDirectory(session.sessionDir);
            } catch (Exception e) {
                log.warn("Failed to cleanup chunked upload session: {}", uploadId, e);
            }
            log.info("Chunked upload aborted: {}", uploadId);
        }
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private static class ChunkUploadSession {
        String uploadId;
        String fileName;
        long fileSize;
        int totalChunks;
        Long projectId;
        String type;
        String versionTag;
        ConcurrentHashMap<Integer, Boolean> receivedChunks;
        Path sessionDir;
    }

    private static class SequenceChunkInputStream extends InputStream {
        private final Path sessionDir;
        private final int totalChunks;
        private int currentChunk = 0;
        private InputStream currentStream;

        SequenceChunkInputStream(Path sessionDir, int totalChunks) {
            this.sessionDir = sessionDir;
            this.totalChunks = totalChunks;
        }

        @Override
        public int read() throws java.io.IOException {
            if (currentStream == null) {
                if (currentChunk >= totalChunks) return -1;
                currentStream = Files.newInputStream(sessionDir.resolve("chunk_" + currentChunk));
            }

            int b = currentStream.read();
            if (b == -1) {
                currentStream.close();
                currentChunk++;
                if (currentChunk >= totalChunks) return -1;
                currentStream = Files.newInputStream(sessionDir.resolve("chunk_" + currentChunk));
                return read();
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws java.io.IOException {
            if (currentStream == null) {
                if (currentChunk >= totalChunks) return -1;
                currentStream = Files.newInputStream(sessionDir.resolve("chunk_" + currentChunk));
            }

            int bytesRead = currentStream.read(b, off, len);
            if (bytesRead == -1) {
                currentStream.close();
                currentChunk++;
                if (currentChunk >= totalChunks) return -1;
                currentStream = Files.newInputStream(sessionDir.resolve("chunk_" + currentChunk));
                return read(b, off, len);
            }
            return bytesRead;
        }

        @Override
        public void close() throws java.io.IOException {
            if (currentStream != null) {
                currentStream.close();
            }
        }
    }
}
