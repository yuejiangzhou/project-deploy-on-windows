package com.company.deploy.controller;

import com.company.deploy.common.Result;
import com.company.deploy.entity.UploadedFile;
import com.company.deploy.enums.FileTypeEnum;
import com.company.deploy.service.InfrastructureService;
import com.company.deploy.service.OperationLogService;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/infra")
@RequiredArgsConstructor
@Slf4j
public class InfrastructureController {

    private final InfrastructureService infrastructureService;
    private final OperationLogService operationLogService;

    private void validateInfraType(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("type 参数不能为空");
        }
        java.util.Set<String> allowedTypes = new java.util.HashSet<>(
                java.util.Arrays.asList("jdk", "mysql", "minio", "nginx", "engine"));
        if (!allowedTypes.contains(type)) {
            throw new IllegalArgumentException("不支持的组件类型: " + type);
        }
    }

    @GetMapping("/{type}/list")
    public Result<List<UploadedFile>> listComponents(@PathVariable String type) {
        validateInfraType(type);
        return Result.success(infrastructureService.listComponents(type));
    }

    @PostMapping("/{type}/upload")
    public Result<UploadedFile> uploadComponent(
            @PathVariable String type,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "version", required = false) String version,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "initState", required = false) String initState,
            @RequestParam(value = "belongsTo", required = false) String belongsTo,
            HttpServletRequest httpRequest) {
        validateInfraType(type);
        try {
            UploadedFile uploaded = infrastructureService.uploadComponent(type, file, version, tag, initState, belongsTo);
            operationLogService.logSuccess("INFRA", "UPLOAD", "COMPONENT", uploaded.getId(),
                    uploaded.getFileName(), "上传组件[" + type + "]: " + uploaded.getFileName(),
                    "admin", httpRequest.getRemoteAddr());
            return Result.success(uploaded);
        } catch (Exception e) {
            log.error("Upload infrastructure component failed", e);
            operationLogService.logFailure("INFRA", "UPLOAD", "COMPONENT", null,
                    file.getOriginalFilename(), "上传组件[" + type + "]失败: " + e.getMessage(),
                    "admin", httpRequest.getRemoteAddr());
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/{type}/upload-folder")
    public Result<UploadedFile> uploadComponentFolder(
            @PathVariable String type,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(defaultValue = "component") String folderName,
            @RequestParam(value = "version", required = false) String version,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "initState", required = false) String initState,
            @RequestParam(value = "belongsTo", required = false) String belongsTo,
            HttpServletRequest httpRequest) {
        validateInfraType(type);
        try {
            UploadedFile uploaded = infrastructureService.uploadComponentFolder(type, files, folderName, version, tag, initState, belongsTo);
            operationLogService.logSuccess("INFRA", "UPLOAD_FOLDER", "COMPONENT", uploaded.getId(),
                    uploaded.getFileName(), "上传组件文件夹[" + type + "]: " + uploaded.getFileName() + " (" + files.size() + " 文件)",
                    "admin", httpRequest.getRemoteAddr());
            return Result.success(uploaded);
        } catch (Exception e) {
            log.error("Upload infrastructure folder failed", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/{type}/upload-zip")
    public Result<UploadedFile> uploadComponentZip(
            @PathVariable String type,
            @RequestParam("file") MultipartFile zipFile,
            @RequestParam(value = "version", required = false) String version,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "initState", required = false) String initState,
            @RequestParam(value = "belongsTo", required = false) String belongsTo,
            HttpServletRequest httpRequest) {
        validateInfraType(type);
        try {
            UploadedFile uploaded = infrastructureService.uploadComponentZip(type, zipFile, version, tag, initState, belongsTo);
            operationLogService.logSuccess("INFRA", "UPLOAD_ZIP", "COMPONENT", uploaded.getId(),
                    uploaded.getFileName(), "ZIP上传解压[" + type + "]: " + uploaded.getFileName(),
                    "admin", httpRequest.getRemoteAddr());
            return Result.success(uploaded);
        } catch (Exception e) {
            log.error("Upload infrastructure zip failed", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{type}/{id}")
    public Result<Void> deleteComponent(@PathVariable String type, @PathVariable Long id,
                                        HttpServletRequest httpRequest) {
        validateInfraType(type);
        try {
            UploadedFile file = infrastructureService.getComponent(id);
            if (file == null) {
                return Result.error(404, "组件不存在");
            }
            infrastructureService.deleteComponent(type, id);
            operationLogService.logSuccess("INFRA", "DELETE", "COMPONENT", id,
                    null, "删除组件[" + type + "]: ID=" + id, "admin", httpRequest.getRemoteAddr());
            return Result.success();
        } catch (Exception e) {
            log.error("Delete infrastructure component failed", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    @PutMapping("/{type}/{id}")
    public Result<UploadedFile> updateComponent(
            @PathVariable String type,
            @PathVariable Long id,
            @RequestBody Map<String, String> updates,
            HttpServletRequest httpRequest) {
        validateInfraType(type);
        try {
            UploadedFile updated = infrastructureService.updateComponent(id, updates);
            operationLogService.logSuccess("INFRA", "UPDATE", "COMPONENT", id,
                    updated.getFileName(), "更新组件[" + type + "]: " + updated.getFileName(),
                    "admin", httpRequest.getRemoteAddr());
            return Result.success(updated);
        } catch (Exception e) {
            log.error("Update infrastructure component failed", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }
}
