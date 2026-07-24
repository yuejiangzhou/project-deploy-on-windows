package com.company.deploy.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class LicenseRenewRequest {

    @NotNull(message = "试用天数不能为空")
    private Integer trialDays;
}
