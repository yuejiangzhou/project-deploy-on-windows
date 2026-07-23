package com.company.deploy.controller;

import com.company.deploy.common.PageResult;
import com.company.deploy.common.Result;
import com.company.deploy.dto.ProjectConfigDTO;
import com.company.deploy.dto.ProjectCreateRequest;
import com.company.deploy.entity.PackageRecord;
import com.company.deploy.entity.Project;
import com.company.deploy.service.OperationLogService;
import com.company.deploy.service.PackageService;
import com.company.deploy.service.ProjectService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final PackageService packageService;
    private final OperationLogService operationLogService;

    @GetMapping
    public Result<PageResult<Project>> getProjects(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        pageNum = Math.max(1, pageNum);
        pageSize = Math.max(1, pageSize);
        return Result.success(projectService.getProjectList(pageNum, pageSize, keyword));
    }

    @GetMapping("/stats")
    public Result<Map<String, Long>> getStats() {
        return Result.success(projectService.getStats());
    }

    @GetMapping("/{id}")
    public Result<Project> getProject(@PathVariable Long id) {
        Project project = projectService.getProjectById(id);
        if (project == null) {
            return Result.error(404, "项目不存在");
        }
        return Result.success(project);
    }

    @PostMapping
    public Result<Project> createProject(@Valid @RequestBody ProjectCreateRequest request,
                                         HttpServletRequest httpRequest) {
        Project project = projectService.createProject(request);
        operationLogService.logSuccess("PROJECT", "CREATE", "PROJECT", project.getId(),
                project.getName(), "创建项目: " + project.getName(), "admin", httpRequest.getRemoteAddr());
        return Result.success(project);
    }

    @PutMapping("/{id}")
    public Result<Project> updateProject(@PathVariable Long id,
                                        @Valid @RequestBody ProjectCreateRequest request,
                                        HttpServletRequest httpRequest) {
        Project project = projectService.updateProject(id, request);
        operationLogService.logSuccess("PROJECT", "UPDATE", "PROJECT", project.getId(),
                project.getName(), "更新项目: " + project.getName(), "admin", httpRequest.getRemoteAddr());
        return Result.success(project);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteProject(@PathVariable Long id, HttpServletRequest httpRequest) {
        Project project = projectService.getProjectById(id);
        if (project == null) {
            return Result.error(404, "项目不存在");
        }
        projectService.deleteProject(id);
        operationLogService.logSuccess("PROJECT", "DELETE", "PROJECT", id,
                project.getName(), "删除项目: " + project.getName(), "admin", httpRequest.getRemoteAddr());
        return Result.success();
    }

    @GetMapping("/{id}/config")
    public Result<ProjectConfigDTO> getConfig(@PathVariable Long id) {
        return Result.success(projectService.getProjectConfig(id));
    }

    @PostMapping("/{id}/config")
    public Result<ProjectConfigDTO> saveConfig(@PathVariable Long id,
                                              @Valid @RequestBody ProjectConfigDTO config,
                                              HttpServletRequest httpRequest) {
        Project project = projectService.getProjectById(id);
        ProjectConfigDTO result = projectService.saveProjectConfig(id, config);
        operationLogService.logSuccess("PROJECT", "SAVE_CONFIG", "PROJECT", id,
                project != null ? project.getName() : String.valueOf(id),
                "保存项目配置", "admin", httpRequest.getRemoteAddr());
        return Result.success(result);
    }

    @GetMapping("/{id}/latest-package")
    public Result<PackageRecord> getLatestPackage(@PathVariable Long id) {
        PackageRecord record = packageService.getLatestSuccessPackage(id);
        return Result.success(record);
    }

    @GetMapping("/{id}/latest-package/download")
    public void downloadLatestPackage(@PathVariable Long id, HttpServletResponse response,
                                      HttpServletRequest httpRequest) throws Exception {
        PackageRecord record = packageService.getLatestSuccessPackage(id);
        if (record == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":404,\"message\":\"暂无打包产物\"}");
            return;
        }
        operationLogService.logSuccess("PACKAGE", "DOWNLOAD", "PACKAGE", record.getId(),
                record.getFileName(), "下载最新包: " + record.getFileName(), "admin", httpRequest.getRemoteAddr());
        String fileName = URLEncoder.encode(record.getFileName(), "UTF-8").replace("+", "%20");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        if (record.getFileSize() != null) {
            response.setContentLengthLong(record.getFileSize());
        }
        try (InputStream is = packageService.downloadPackage(record.getId());
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.flush();
        }
    }
}
