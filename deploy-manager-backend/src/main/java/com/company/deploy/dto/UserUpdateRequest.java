package com.company.deploy.dto;

import lombok.Data;

@Data
public class UserUpdateRequest {

    private String displayName;

    private String role;

    private String status;
}
