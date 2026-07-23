package com.company.deploy.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("package_task")
public class PackageTask {

    @TableId
    private String id;

    private Long projectId;

    private String status;

    private Integer progress;

    private String currentStep;

    private String password;

    private LocalDateTime createdAt;

    private LocalDateTime finishedAt;
}
