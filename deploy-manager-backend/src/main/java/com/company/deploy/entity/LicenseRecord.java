package com.company.deploy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("license_record")
public class LicenseRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String licenseId;

    private String type;

    private String customerName;

    private LocalDate issueDate;

    private LocalDate expireDate;

    private Integer trialDays;

    private String dataJson;

    private String signature;

    private String minioBucket;

    private String minioLicObjectKey;

    private String minioTimestampObjectKey;

    private Long parentId;

    private String status;

    private String statusNote;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean deleted;
}
