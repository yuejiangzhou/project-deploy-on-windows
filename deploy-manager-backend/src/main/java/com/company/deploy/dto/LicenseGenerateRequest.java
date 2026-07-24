package com.company.deploy.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class LicenseGenerateRequest {

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    @NotBlank(message = "客户名称不能为空")
    private String customerName;

    @NotNull(message = "试用天数不能为空")
    private Integer trialDays;

    @NotBlank(message = "License类型不能为空")
    private String type;
}
