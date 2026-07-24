package com.company.deploy.enums;

import lombok.Getter;

@Getter
public enum FileTypeEnum {
    JAR("jar", "JAR包"),
    VUE("vue", "Vue产物"),
    SQL("sql", "SQL脚本"),
    NGINX_CONF("nginx_conf", "Nginx配置"),
    JDK("jdk", "JDK"),
    MYSQL("mysql", "MySQL"),
    MINIO("minio", "MinIO"),
    NGINX("nginx", "Nginx"),
    ENGINE("engine", "引擎"),
    REDIS("redis", "Redis");

    private final String code;
    private final String desc;

    FileTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static FileTypeEnum of(String code) {
        for (FileTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
