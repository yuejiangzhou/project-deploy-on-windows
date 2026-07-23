package com.company.deploy.controller;

import com.company.deploy.common.Result;
import com.company.deploy.common.PageResult;
import com.company.deploy.dto.PackageProgressDTO;
import com.company.deploy.dto.PackageStartRequest;
import com.company.deploy.entity.PackageRecord;
import com.company.deploy.service.OperationLogService;
import com.company.deploy.service.PackageService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;
    private final OperationLogService operationLogService;

    @PostMapping("/package/start")
    public Result<Map<String, String>> startPackage(@Valid @RequestBody PackageStartRequest request,
                                                     HttpServletRequest httpRequest) {
        try {
            String taskId = packageService.startPackage(request.getProjectId(), request.getPassword());
            operationLogService.logSuccess("PACKAGE", "START", "PROJECT", request.getProjectId(),
                    null, "启动打包任务: " + taskId, "admin", httpRequest.getRemoteAddr());
            return Result.success(Collections.singletonMap("taskId", taskId));
        } catch (IllegalArgumentException e) {
            operationLogService.logFailure("PACKAGE", "START", "PROJECT", request.getProjectId(),
                    null, "打包参数错误: " + e.getMessage(), "admin", httpRequest.getRemoteAddr());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            operationLogService.logFailure("PACKAGE", "START", "PROJECT", request.getProjectId(),
                    null, "打包失败: " + e.getMessage(), "admin", httpRequest.getRemoteAddr());
            // 项目不存在等业务异常返回 404
            String msg = e.getMessage();
            if (msg != null && msg.contains("不存在")) {
                return Result.error(404, msg);
            }
            return Result.error(500, msg);
        }
    }

    @GetMapping("/package/progress/{taskId}")
    public Result<PackageProgressDTO> getProgress(@PathVariable String taskId) {
        return Result.success(packageService.getProgress(taskId));
    }

    @GetMapping("/packages")
    public Result<PageResult<PackageRecord>> getPackages(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        if (projectId == null) {
            return Result.error(400, "projectId 不能为空");
        }
        pageNum = Math.max(1, pageNum);
        pageSize = Math.max(1, pageSize);
        return Result.success(packageService.getPackageRecords(projectId, status, pageNum, pageSize));
    }

    @GetMapping("/packages/{id}/download")
    public void downloadPackage(@PathVariable Long id, HttpServletResponse response,
                                HttpServletRequest httpRequest) throws Exception {
        PackageRecord record = packageService.getPackageRecord(id);
        if (record == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        operationLogService.logSuccess("PACKAGE", "DOWNLOAD", "PACKAGE", id,
                record.getFileName(), "下载包: " + record.getFileName(), "admin", httpRequest.getRemoteAddr());

        response.setContentType("application/zip");
        String encodedName = URLEncoder.encode(record.getFileName(), "UTF-8")
                .replace("+", "%20");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + encodedName + "\"");
        if (record.getFileSize() != null) {
            response.setContentLengthLong(record.getFileSize());
        }

        try (InputStream is = packageService.downloadPackage(id)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                response.getOutputStream().write(buffer, 0, len);
            }
            response.flushBuffer();
        }
    }

    @DeleteMapping("/packages/{id}")
    public Result<Void> deletePackage(@PathVariable Long id, HttpServletRequest httpRequest) throws Exception {
        PackageRecord record = packageService.getPackageRecord(id);
        if (record == null) {
            return Result.error(404, "打包记录不存在");
        }
        packageService.deletePackage(id);
        operationLogService.logSuccess("PACKAGE", "DELETE", "PACKAGE", id,
                record.getFileName(), "删除包: " + record.getFileName(), "admin", httpRequest.getRemoteAddr());
        return Result.success();
    }
}
