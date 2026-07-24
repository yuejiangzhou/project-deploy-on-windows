package com.company.deploy.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PackageStartRequest {

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    /** ZIP加密密码，仅当encrypted=true时必填 */
    private String password;

    /** 是否加密ZIP，默认false（不加密） */
    private Boolean encrypted;

    private Long licenseId;
}
