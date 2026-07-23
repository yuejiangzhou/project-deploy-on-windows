package com.company.deploy.controller;

import com.company.deploy.common.Result;
import com.company.deploy.dto.FileTreeNode;
import com.company.deploy.entity.UploadedFile;
import com.company.deploy.service.ChunkedUploadService;
import com.company.deploy.service.FileService;
import com.company.deploy.service.OperationLogService;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final OperationLogService operationLogService;
    private final ChunkedUploadService chunkedUploadService;

    private void validateType(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("type 参数不能为空");
        }
        // Validate against known types
        java.util.Set<String> allowedTypes = new java.util.HashSet<>(java.util.Arrays.asList("jar", "vue"));
        if (!allowedTypes.contains(type)) {
            throw new IllegalArgumentException("不支持的 type: " + type);
        }
    }

    @GetMapping("/tree")
    public Result<List<FileTreeNode>> getFileTree(@RequestParam(required = false) Long projectId) {
        return Result.success(fileService.getFileTree(projectId));
    }

    @GetMapping("/list")
    public Result<List<UploadedFile>> listFiles(
            @RequestParam Long projectId,
            @RequestParam String type,
            @RequestParam(required = false) String date) {
        validateType(type);
        return Result.success(fileService.listFiles(projectId, type, date));
    }

    @GetMapping("/paths")
    public Result<List<UploadedFile>> getPathOptions(
            @RequestParam Long projectId,
            @RequestParam String type) {
        validateType(type);
        return Result.success(fileService.getPathOptions(projectId, type));
    }

    @PostMapping("/upload")
    public Result<UploadedFile> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long projectId,
            @RequestParam String type,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String versionTag,
            HttpServletRequest httpRequest) throws Exception {
        validateType(type);
        UploadedFile uploaded = fileService.uploadFile(file, projectId, type, date, versionTag);
        operationLogService.logSuccess("FILE", "UPLOAD", "FILE", uploaded.getId(),
                uploaded.getFileName(), "上传文件: " + uploaded.getFileName() + " (类型: " + type + ")",
                "admin", httpRequest.getRemoteAddr());
        return Result.success(uploaded);
    }

    @PostMapping("/upload-folder")
    public Result<UploadedFile> uploadFolder(
            @RequestParam("file") List<MultipartFile> files,
            @RequestParam Long projectId,
            @RequestParam String type,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "dist") String folderName,
            @RequestParam(required = false) String versionTag,
            HttpServletRequest httpRequest) throws Exception {
        validateType(type);
        UploadedFile uploaded = fileService.uploadFolder(files, projectId, type, date, folderName, versionTag);
        operationLogService.logSuccess("FILE", "UPLOAD_FOLDER", "FILE", uploaded.getId(),
                uploaded.getFileName(), "上传文件夹: " + uploaded.getFileName() + " (" + files.size() + " 个文件)",
                "admin", httpRequest.getRemoteAddr());
        return Result.success(uploaded);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteFile(@PathVariable Long id, HttpServletRequest httpRequest) throws Exception {
        fileService.deleteFile(id);
        operationLogService.logSuccess("FILE", "DELETE", "FILE", id,
                null, "删除文件ID: " + id, "admin", httpRequest.getRemoteAddr());
        return Result.success();
    }

    @PostMapping("/upload/chunk-init")
    public Result<Map<String, Object>> initChunkedUpload(
            @RequestParam String fileName,
            @RequestParam long fileSize,
            @RequestParam int totalChunks,
            @RequestParam Long projectId,
            @RequestParam String type,
            @RequestParam(required = false) String versionTag,
            HttpServletRequest httpRequest) {
        validateType(type);
        Map<String, Object> result = chunkedUploadService.initUpload(fileName, fileSize, totalChunks, projectId, type, versionTag);
        operationLogService.logSuccess("FILE", "CHUNK_INIT", "FILE", null,
                fileName, "初始化分片上传: " + fileName + " (" + totalChunks + " 片)",
                "admin", httpRequest.getRemoteAddr());
        return Result.success(result);
    }

    @PostMapping("/upload/chunk")
    public Result<Void> uploadChunk(
            @RequestParam String uploadId,
            @RequestParam int chunkIndex,
            @RequestParam("chunk") MultipartFile chunkFile) {
        try {
            chunkedUploadService.uploadChunk(uploadId, chunkIndex, chunkFile);
            return Result.success();
        } catch (Exception e) {
            return Result.error("分片上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/upload/chunk-complete")
    public Result<UploadedFile> completeChunkedUpload(
            @RequestParam String uploadId,
            HttpServletRequest httpRequest) {
        try {
            UploadedFile uploaded = chunkedUploadService.completeUpload(uploadId);
            operationLogService.logSuccess("FILE", "CHUNK_COMPLETE", "FILE", uploaded.getId(),
                    uploaded.getFileName(), "分片上传完成: " + uploaded.getFileName(),
                    "admin", httpRequest.getRemoteAddr());
            return Result.success(uploaded);
        } catch (Exception e) {
            return Result.error("合并分片失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/upload/chunk-abort/{uploadId}")
    public Result<Void> abortChunkedUpload(@PathVariable String uploadId) {
        chunkedUploadService.abortUpload(uploadId);
        return Result.success();
    }
}
