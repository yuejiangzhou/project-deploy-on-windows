package com.company.deploy.dto;

import lombok.Data;

@Data
public class ProjectConfigDTO {

    private Long jarFileId;
    private Long vueFolderId;
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
    private Boolean includeJdk;
    private Boolean includeMysql;
    private Boolean includeMinio;
    private Boolean includeNginx;
    private Integer enginePort;

    private Long jdkComponentId;
    private Long mysqlComponentId;
    private Long minioComponentId;
    private Long nginxComponentId;
    private Long engineComponentId;

    private String jarFileName;
    private Long jarFileSize;
    private String jarUploadDate;
    private String vueFileName;
    private Long vueFileSize;
    private String vueUploadDate;
    private String jdkFileName;
    private Long jdkFileSize;
    private String jdkVersion;
    private String mysqlFileName;
    private Long mysqlFileSize;
    private String mysqlVersion;
    private String minioFileName;
    private Long minioFileSize;
    private String minioVersion;
    private String nginxFileName;
    private Long nginxFileSize;
    private String nginxVersion;
    private String engineFileName;
    private Long engineFileSize;
    private String engineVersion;
}
