package com.company.deploy.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectCreateRequest {

    @NotBlank(message = "项目名称不能为空")
    private String name;

    private String description;
}
