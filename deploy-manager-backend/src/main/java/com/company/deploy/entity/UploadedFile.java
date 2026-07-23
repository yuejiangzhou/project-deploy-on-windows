package com.company.deploy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("uploaded_file")
public class UploadedFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String fileType;

    private String fileName;

    private Long fileSize;

    private String minioBucket;

    private String minioObjectKey;

    private Boolean isFolder;

    private String folderPath;

    private LocalDate uploadDate;

    private String versionTag;

    private String version;

    private String tag;

    private String initState;

    private String belongsTo;

    private LocalDateTime createdAt;

    @TableLogic
    private Boolean deleted;
}
