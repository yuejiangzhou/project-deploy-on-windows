package com.company.deploy.service;

import com.company.deploy.entity.Project;
import com.company.deploy.entity.UploadedFile;
import com.company.deploy.mapper.UploadedFileMapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScriptTemplateService {

    private final Configuration freemarkerConfig;
    private final UploadedFileMapper uploadedFileMapper;

    public void generateScripts(Project project, Path outputDir) throws Exception {
        Map<String, Object> model = buildTemplateModel(project);

        Path servicesDir = outputDir.resolve("services");
        Path pidDir = outputDir.resolve("pid");
        Files.createDirectories(servicesDir);
        Files.createDirectories(pidDir);

        generateScript("start.ps1.ftl", model, outputDir.resolve("start.ps1"));
        generateScript("stop.ps1.ftl", model, outputDir.resolve("stop.ps1"));

        generateScript("services/start-jar.ps1.ftl", model, servicesDir.resolve("start-jar.ps1"));
        generateScript("services/stop-jar.ps1.ftl", model, servicesDir.resolve("stop-jar.ps1"));
        generateScript("services/start-mysql.ps1.ftl", model, servicesDir.resolve("start-mysql.ps1"));
        generateScript("services/stop-mysql.ps1.ftl", model, servicesDir.resolve("stop-mysql.ps1"));
        generateScript("services/start-minio.ps1.ftl", model, servicesDir.resolve("start-minio.ps1"));
        generateScript("services/stop-minio.ps1.ftl", model, servicesDir.resolve("stop-minio.ps1"));
        generateScript("services/start-nginx.ps1.ftl", model, servicesDir.resolve("start-nginx.ps1"));
        generateScript("services/stop-nginx.ps1.ftl", model, servicesDir.resolve("stop-nginx.ps1"));

        if (Boolean.TRUE.equals(project.getEngineEnabled())) {
            generateScript("services/start-engine.ps1.ftl", model, servicesDir.resolve("start-engine.ps1"));
            generateScript("services/stop-engine.ps1.ftl", model, servicesDir.resolve("stop-engine.ps1"));
        }

        if (Boolean.TRUE.equals(project.getIncludeRedis())) {
            generateScript("services/start-redis.ps1.ftl", model, servicesDir.resolve("start-redis.ps1"));
            generateScript("services/stop-redis.ps1.ftl", model, servicesDir.resolve("stop-redis.ps1"));
        }

        log.info("Scripts generated to: {}", outputDir);
    }

    private Map<String, Object> buildTemplateModel(Project project) {
        Map<String, Object> model = new HashMap<>();

        model.put("projectName", project.getName());
        model.put("generateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        String jarFileName = "app.jar";
        if (project.getJarFileId() != null) {
            UploadedFile jarFile = uploadedFileMapper.selectById(project.getJarFileId());
            if (jarFile != null) {
                jarFileName = jarFile.getFileName();
            }
        }
        model.put("jarFileName", jarFileName);

        model.put("appPort", project.getAppPort());
        model.put("jvmParams", project.getJvmParams());
        model.put("mysqlPort", project.getMysqlPort());
        model.put("minioApiPort", project.getMinioApiPort());
        model.put("minioConsolePort", project.getMinioConsolePort());
        model.put("nginxHttpPort", project.getNginxHttpPort());
        model.put("engineEnabled", Boolean.TRUE.equals(project.getEngineEnabled()));
        model.put("enginePort", project.getEnginePort());

        // 账密注入相关字段（纯净版需要注入，已初始化版跳过）
        model.put("dbName", project.getDbName() != null ? project.getDbName() : "");
        model.put("dbUsername", project.getDbUsername() != null ? project.getDbUsername() : "");
        model.put("dbPassword", project.getDbPassword() != null ? project.getDbPassword() : "");
        model.put("minioAccessKey", project.getMinioAccessKey() != null ? project.getMinioAccessKey() : "");
        model.put("minioSecretKey", project.getMinioSecretKey() != null ? project.getMinioSecretKey() : "");
        model.put("mysqlConfigState", project.getMysqlConfigState() != null ? project.getMysqlConfigState() : "clean");
        model.put("minioConfigState", project.getMinioConfigState() != null ? project.getMinioConfigState() : "clean");

        boolean mysqlEnabled = Boolean.TRUE.equals(project.getIncludeMysql());
        boolean minioEnabled = Boolean.TRUE.equals(project.getIncludeMinio());
        boolean nginxEnabled = Boolean.TRUE.equals(project.getIncludeNginx());
        boolean redisEnabled = Boolean.TRUE.equals(project.getIncludeRedis());
        model.put("mysqlEnabled", mysqlEnabled);
        model.put("minioEnabled", minioEnabled);
        model.put("nginxEnabled", nginxEnabled);
        model.put("redisEnabled", redisEnabled);
        model.put("redisPort", project.getRedisPort() != null ? project.getRedisPort() : 6379);

        int serviceCount = 0;
        if (mysqlEnabled) serviceCount++;
        if (minioEnabled) serviceCount++;
        if (nginxEnabled) serviceCount++;
        if (redisEnabled) serviceCount++;
        if (Boolean.TRUE.equals(project.getEngineEnabled())) {
            serviceCount++;
        }
        model.put("serviceCount", serviceCount);

        return model;
    }

    private void generateScript(String templateName, Map<String, Object> model, Path outputPath) throws Exception {
        Template template = freemarkerConfig.getTemplate(templateName);
        try (StringWriter writer = new StringWriter()) {
            template.process(model, writer);
            Files.write(outputPath, writer.toString().getBytes(StandardCharsets.UTF_8));
        }
        log.debug("Generated script: {}", outputPath.getFileName());
    }
}
