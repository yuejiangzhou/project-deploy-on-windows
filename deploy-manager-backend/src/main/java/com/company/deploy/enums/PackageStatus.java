package com.company.deploy.enums;

import lombok.Getter;

@Getter
public enum PackageStatus {
    PENDING("PENDING", "排队中"),
    RUNNING("RUNNING", "打包中"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败");

    private final String code;
    private final String desc;

    PackageStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
