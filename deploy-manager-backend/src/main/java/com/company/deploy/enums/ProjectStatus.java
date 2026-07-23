package com.company.deploy.enums;

import lombok.Getter;

@Getter
public enum ProjectStatus {
    CONFIGURING("CONFIGURING", "配置中"),
    READY("READY", "已就绪"),
    ARCHIVED("ARCHIVED", "已归档");

    private final String code;
    private final String desc;

    ProjectStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
