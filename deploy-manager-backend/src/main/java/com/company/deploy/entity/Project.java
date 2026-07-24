package com.company.deploy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project")
public class Project {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String status;

    private Long jarFileId;

    private Long vueFolderId;

    @TableField(exist = false)
    private String jarFileName;

    @TableField(exist = false)
    private Boolean vueConfigured;

    private Long nginxConfFileId;

    private String nginxConfContent;

    private Integer appPort;

    private String jvmParams;

    private Integer mysqlPort;

    private String dbName;

    private String dbUsername;

    private String dbPassword;

    private Integer minioApiPort;

    private Integer minioConsolePort;

    private String minioAccessKey;

    private String minioSecretKey;

    private Integer nginxHttpPort;

    private Boolean engineEnabled;

    private Integer enginePort;

    private Boolean includeJdk;

    private Boolean includeMysql;

    private Boolean includeMinio;

    private Boolean includeNginx;

    private Boolean includeRedis;

    private Long jdkComponentId;

    private Long mysqlComponentId;

    private Long minioComponentId;

    private Long nginxComponentId;

    private Long engineComponentId;

    private Long redisComponentId;

    private String jdkVersion;

    private String mysqlVersion;

    private String mysqlConfigState;

    private String minioVersion;

    private String minioConfigState;

    private String nginxVersion;

    private String engineVersion;

    private Integer redisPort;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean deleted;
}
