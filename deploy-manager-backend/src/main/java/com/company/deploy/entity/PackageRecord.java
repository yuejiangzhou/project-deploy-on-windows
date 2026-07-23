package com.company.deploy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("package_record")
public class PackageRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String fileName;

    private Long fileSize;

    private Boolean encrypted;

    private String minioBucket;

    private String minioObjectKey;

    private String status;

    private String taskId;

    private LocalDateTime createdAt;

    @TableLogic
    private Boolean deleted;
}
