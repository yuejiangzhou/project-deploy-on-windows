package com.company.deploy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String module;

    private String action;

    private String targetType;

    private Long targetId;

    private String targetName;

    private String detail;

    private String status;

    private String operator;

    private String ip;

    private LocalDateTime createdAt;
}
