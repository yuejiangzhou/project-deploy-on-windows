package com.company.deploy.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class LicenseRecordDTO {

    private Long id;

    private Long projectId;

    private String licenseId;

    private String type;

    private String customerName;

    private LocalDate issueDate;

    private LocalDate expireDate;

    private Integer trialDays;

    private Long parentId;

    private String status;

    private String statusNote;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
