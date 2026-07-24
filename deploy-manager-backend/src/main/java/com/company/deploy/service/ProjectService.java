package com.company.deploy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.deploy.common.PageResult;
import com.company.deploy.dto.ProjectConfigDTO;
import com.company.deploy.dto.ProjectCreateRequest;
import com.company.deploy.entity.Project;
import com.company.deploy.entity.PackageRecord;
import com.company.deploy.entity.UploadedFile;
import com.company.deploy.enums.ProjectStatus;
import com.company.deploy.mapper.ProjectMapper;
import com.company.deploy.mapper.PackageRecordMapper;
import com.company.deploy.mapper.UploadedFileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final PackageRecordMapper packageRecordMapper;
    private final UploadedFileMapper uploadedFileMapper;

    public PageResult<Project> getProjectList(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Project::getDeleted, false);
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(Project::getName, keyword.trim());
        }
        wrapper.orderByDesc(Project::getCreatedAt);

        Page<Project> page = projectMapper.selectPage(Page.of(pageNum, pageSize), wrapper);

        for (Project project : page.getRecords()) {
            if (project.getJarFileId() != null) {
                UploadedFile jarFile = uploadedFileMapper.selectById(project.getJarFileId());
                if (jarFile != null) {
                    project.setJarFileName(jarFile.getFileName());
                }
            }
            project.setVueConfigured(project.getVueFolderId() != null);
        }

        return PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    public Project getProjectById(Long id) {
        Project project = projectMapper.selectById(id);
        if (project != null) {
            if (project.getJarFileId() != null) {
                UploadedFile jarFile = uploadedFileMapper.selectById(project.getJarFileId());
                if (jarFile != null) {
                    project.setJarFileName(jarFile.getFileName());
                }
            }
            project.setVueConfigured(project.getVueFolderId() != null);
        }
        return project;
    }

    @Transactional
    public Project createProject(ProjectCreateRequest request) {
        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setStatus(ProjectStatus.CONFIGURING.getCode());
        project.setAppPort(8080);
        project.setJvmParams("-Xms512m -Xmx2048m");
        project.setMysqlPort(3306);
        project.setMinioApiPort(9000);
        project.setMinioConsolePort(9001);
        project.setNginxHttpPort(80);
        project.setEngineEnabled(false);
        project.setEnginePort(8090);
        project.setIncludeJdk(true);
        project.setIncludeMysql(false);
        project.setIncludeMinio(false);
        project.setIncludeNginx(false);
        project.setIncludeRedis(false);
        project.setJdkVersion("17.0.2");
        project.setMysqlVersion("8.0.35");
        project.setMysqlConfigState("clean");
        project.setMinioVersion("2024.06");
        project.setMinioConfigState("clean");
        project.setNginxVersion("1.24");
        project.setEngineVersion("v3.2");
        project.setRedisPort(6379);
        try {
            projectMapper.insert(project);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("项目名已存在");
        }
        return project;
    }

    @Transactional
    public Project updateProject(Long id, ProjectCreateRequest request) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        projectMapper.updateById(project);
        return project;
    }

    @Transactional
    public void deleteProject(Long id) {
        projectMapper.deleteById(id);
    }

    public ProjectConfigDTO getProjectConfig(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }
        ProjectConfigDTO dto = new ProjectConfigDTO();
        dto.setJarFileId(project.getJarFileId());
        dto.setVueFolderId(project.getVueFolderId());
        dto.setNginxConfFileId(project.getNginxConfFileId());
        dto.setNginxConfContent(project.getNginxConfContent());
        dto.setAppPort(project.getAppPort());
        dto.setJvmParams(project.getJvmParams());
        dto.setMysqlPort(project.getMysqlPort());
        dto.setDbName(project.getDbName());
        dto.setDbUsername(project.getDbUsername());
        dto.setDbPassword(project.getDbPassword());
        dto.setMinioApiPort(project.getMinioApiPort());
        dto.setMinioConsolePort(project.getMinioConsolePort());
        dto.setMinioAccessKey(project.getMinioAccessKey());
        dto.setMinioSecretKey(project.getMinioSecretKey());
        dto.setNginxHttpPort(project.getNginxHttpPort());
        dto.setEngineEnabled(project.getEngineEnabled());
        dto.setEnginePort(project.getEnginePort());
        dto.setIncludeJdk(project.getIncludeJdk());
        dto.setIncludeMysql(project.getIncludeMysql());
        dto.setIncludeMinio(project.getIncludeMinio());
        dto.setIncludeNginx(project.getIncludeNginx());
        dto.setIncludeRedis(project.getIncludeRedis());
        dto.setJdkComponentId(project.getJdkComponentId());
        dto.setMysqlComponentId(project.getMysqlComponentId());
        dto.setMinioComponentId(project.getMinioComponentId());
        dto.setNginxComponentId(project.getNginxComponentId());
        dto.setEngineComponentId(project.getEngineComponentId());
        dto.setRedisComponentId(project.getRedisComponentId());
        dto.setRedisPort(project.getRedisPort());

        fillFileInfo(dto, project.getJarFileId(), "jar");
        fillFileInfo(dto, project.getVueFolderId(), "vue");
        fillFileInfo(dto, project.getJdkComponentId(), "jdk");
        fillFileInfo(dto, project.getMysqlComponentId(), "mysql");
        fillFileInfo(dto, project.getMinioComponentId(), "minio");
        fillFileInfo(dto, project.getNginxComponentId(), "nginx");
        fillFileInfo(dto, project.getEngineComponentId(), "engine");
        fillFileInfo(dto, project.getRedisComponentId(), "redis");

        return dto;
    }

    private void fillFileInfo(ProjectConfigDTO dto, Long fileId, String prefix) {
        if (fileId == null) return;
        UploadedFile file = uploadedFileMapper.selectById(fileId);
        if (file == null) return;
        String fileName = file.getFileName();
        Long fileSize = file.getFileSize();
        String uploadDate = file.getUploadDate() != null ? file.getUploadDate().toString() : null;
        String version = file.getVersion();

        switch (prefix) {
            case "jar":
                dto.setJarFileName(fileName);
                dto.setJarFileSize(fileSize);
                dto.setJarUploadDate(uploadDate);
                break;
            case "vue":
                dto.setVueFileName(fileName);
                dto.setVueFileSize(fileSize);
                dto.setVueUploadDate(uploadDate);
                break;
            case "jdk":
                dto.setJdkFileName(fileName);
                dto.setJdkFileSize(fileSize);
                dto.setJdkVersion(version);
                break;
            case "mysql":
                dto.setMysqlFileName(fileName);
                dto.setMysqlFileSize(fileSize);
                dto.setMysqlVersion(version);
                break;
            case "minio":
                dto.setMinioFileName(fileName);
                dto.setMinioFileSize(fileSize);
                dto.setMinioVersion(version);
                break;
            case "nginx":
                dto.setNginxFileName(fileName);
                dto.setNginxFileSize(fileSize);
                dto.setNginxVersion(version);
                break;
            case "engine":
                dto.setEngineFileName(fileName);
                dto.setEngineFileSize(fileSize);
                dto.setEngineVersion(version);
                break;
            case "redis":
                dto.setRedisFileName(fileName);
                dto.setRedisFileSize(fileSize);
                dto.setRedisVersion(version);
                break;
        }
    }

    @Transactional
    public ProjectConfigDTO saveProjectConfig(Long id, ProjectConfigDTO config) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }

        project.setJarFileId(config.getJarFileId());
        project.setVueFolderId(config.getVueFolderId());
        project.setNginxConfFileId(config.getNginxConfFileId());
        project.setNginxConfContent(config.getNginxConfContent());
        project.setAppPort(config.getAppPort());
        project.setJvmParams(config.getJvmParams());
        project.setMysqlPort(config.getMysqlPort());
        project.setDbName(config.getDbName());
        project.setDbUsername(config.getDbUsername());
        project.setDbPassword(config.getDbPassword());
        project.setMinioApiPort(config.getMinioApiPort());
        project.setMinioConsolePort(config.getMinioConsolePort());
        project.setMinioAccessKey(config.getMinioAccessKey());
        project.setMinioSecretKey(config.getMinioSecretKey());
        project.setNginxHttpPort(config.getNginxHttpPort());
        project.setEngineEnabled(config.getEngineEnabled());
        project.setEnginePort(config.getEnginePort());
        if (config.getIncludeJdk() != null) project.setIncludeJdk(config.getIncludeJdk());
        if (config.getIncludeMysql() != null) project.setIncludeMysql(config.getIncludeMysql());
        if (config.getIncludeMinio() != null) project.setIncludeMinio(config.getIncludeMinio());
        if (config.getIncludeNginx() != null) project.setIncludeNginx(config.getIncludeNginx());
        if (config.getIncludeRedis() != null) project.setIncludeRedis(config.getIncludeRedis());
        if (config.getJdkComponentId() != null) project.setJdkComponentId(config.getJdkComponentId());
        if (config.getMysqlComponentId() != null) project.setMysqlComponentId(config.getMysqlComponentId());
        if (config.getMinioComponentId() != null) project.setMinioComponentId(config.getMinioComponentId());
        if (config.getNginxComponentId() != null) project.setNginxComponentId(config.getNginxComponentId());
        if (config.getEngineComponentId() != null) project.setEngineComponentId(config.getEngineComponentId());
        if (config.getRedisComponentId() != null) project.setRedisComponentId(config.getRedisComponentId());
        if (config.getRedisPort() != null) project.setRedisPort(config.getRedisPort());

        updateProjectStatus(project);

        projectMapper.updateById(project);
        return config;
    }

    private void updateProjectStatus(Project project) {
        boolean isReady = project.getJarFileId() != null
                && project.getAppPort() != null
                && project.getMysqlPort() != null
                && project.getMinioApiPort() != null
                && project.getNginxHttpPort() != null;

        String currentStatus = project.getStatus();
        if (ProjectStatus.ARCHIVED.getCode().equals(currentStatus)) {
            return;
        }
        if (isReady) {
            project.setStatus(ProjectStatus.READY.getCode());
        } else {
            project.setStatus(ProjectStatus.CONFIGURING.getCode());
        }
    }

    public Map<String, Long> getStats() {
        Long totalProjects = projectMapper.selectCount(
                new LambdaQueryWrapper<Project>().eq(Project::getDeleted, false));
        Long readyProjects = projectMapper.selectCount(
                new LambdaQueryWrapper<Project>().eq(Project::getDeleted, false).eq(Project::getStatus, "READY"));
        Long configuringProjects = projectMapper.selectCount(
                new LambdaQueryWrapper<Project>().eq(Project::getDeleted, false).eq(Project::getStatus, "CONFIGURING"));
        Long archivedProjects = projectMapper.selectCount(
                new LambdaQueryWrapper<Project>().eq(Project::getDeleted, false).eq(Project::getStatus, "ARCHIVED"));
        // 已打包数只统计成功的记录，不包含失败
        Long totalPackages = packageRecordMapper.selectCount(
                new LambdaQueryWrapper<PackageRecord>()
                        .eq(PackageRecord::getDeleted, false)
                        .eq(PackageRecord::getStatus, "SUCCESS"));

        Map<String, Long> stats = new HashMap<>();
        stats.put("totalProjects", totalProjects);
        stats.put("readyProjects", readyProjects);
        stats.put("configuringProjects", configuringProjects);
        stats.put("archivedProjects", archivedProjects);
        stats.put("totalPackages", totalPackages);
        return stats;
    }
}
