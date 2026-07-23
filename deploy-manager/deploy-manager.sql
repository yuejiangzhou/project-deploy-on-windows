-- ============================================
-- DeployManager 数据库建表脚本
-- 数据库: MySQL 8.0
-- 字符集: utf8mb4
-- ============================================

CREATE DATABASE IF NOT EXISTS `deploy_manager`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE `deploy_manager`;

-- ----------------------------------------
-- 1. 项目表
-- ----------------------------------------
CREATE TABLE `project` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`           VARCHAR(100) NOT NULL                COMMENT '项目名称',
  `description`    VARCHAR(500) DEFAULT NULL              COMMENT '项目描述',
  `status`         VARCHAR(20)  NOT NULL DEFAULT 'CONFIGURING' COMMENT '状态: CONFIGURING/READY/ARCHIVED',
  `jar_file_id`    BIGINT       DEFAULT NULL              COMMENT '选中的JAR包文件ID',
  `vue_folder_id`  BIGINT       DEFAULT NULL              COMMENT '选中的Vue产物文件夹ID',
  `sql_file_id`    BIGINT       DEFAULT NULL              COMMENT '选中的SQL脚本文件ID',
  `nginx_conf_file_id` BIGINT   DEFAULT NULL              COMMENT '选中的Nginx配置文件ID',
  `app_port`       INT          NOT NULL DEFAULT 8080     COMMENT '应用端口',
  `jvm_params`     VARCHAR(500) NOT NULL DEFAULT '-Xms512m -Xmx2048m' COMMENT 'JVM参数',
  `mysql_port`     INT          NOT NULL DEFAULT 3306     COMMENT 'MySQL端口',
  `minio_api_port` INT          NOT NULL DEFAULT 9000     COMMENT 'MinIO API端口',
  `minio_console_port` INT     NOT NULL DEFAULT 9001     COMMENT 'MinIO Console端口',
  `nginx_http_port` INT        NOT NULL DEFAULT 80       COMMENT 'Nginx HTTP端口',
  `engine_enabled` TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '是否包含引擎: 0否 1是',
  `engine_port`    INT          NOT NULL DEFAULT 8090     COMMENT '引擎端口',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- ----------------------------------------
-- 2. 上传文件表
-- ----------------------------------------
CREATE TABLE `uploaded_file` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id`      BIGINT       DEFAULT NULL              COMMENT '所属项目ID(基础组件为空)',
  `file_type`       VARCHAR(20)  NOT NULL                COMMENT '文件类型: jar/vue/sql/nginx_conf/jdk/mysql/minio/nginx/engine',
  `file_name`       VARCHAR(255) NOT NULL                COMMENT '文件/文件夹名称',
  `file_size`       BIGINT       DEFAULT NULL              COMMENT '文件大小(字节)',
  `minio_bucket`    VARCHAR(100) DEFAULT NULL              COMMENT 'MinIO存储桶名',
  `minio_object_key` VARCHAR(500) DEFAULT NULL              COMMENT 'MinIO对象键',
  `is_folder`       TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '是否为文件夹: 0否 1是',
  `folder_path`     VARCHAR(500) DEFAULT NULL              COMMENT '文件夹路径，如 ecom/jar/2026-06-28',
  `upload_date`     DATE         DEFAULT NULL              COMMENT '上传日期(用于三级目录的日期级别)',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `deleted`         TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_project_type` (`project_id`, `file_type`),
  KEY `idx_folder_path` (`folder_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上传文件表';

-- ----------------------------------------
-- 3. 打包产物表
-- ----------------------------------------
CREATE TABLE `package_record` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id`    BIGINT       NOT NULL                COMMENT '所属项目ID',
  `file_name`     VARCHAR(255) NOT NULL                COMMENT 'ZIP文件名',
  `file_size`     BIGINT       DEFAULT NULL              COMMENT 'ZIP文件大小(字节)',
  `encrypted`     TINYINT(1)   NOT NULL DEFAULT 1       COMMENT '是否加密: 0否 1是',
  `minio_bucket`  VARCHAR(100) DEFAULT NULL              COMMENT 'MinIO存储桶名',
  `minio_object_key` VARCHAR(500) DEFAULT NULL          COMMENT 'MinIO对象键',
  `status`        VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS' COMMENT '状态: SUCCESS/FAILED',
  `task_id`       VARCHAR(36)  DEFAULT NULL              COMMENT '打包任务ID',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted`       TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打包产物表';

-- ----------------------------------------
-- 4. 打包任务表（用于进度追踪）
-- ----------------------------------------
CREATE TABLE `package_task` (
  `id`            VARCHAR(36)  NOT NULL                COMMENT '任务ID(UUID)',
  `project_id`    BIGINT       NOT NULL                COMMENT '所属项目ID',
  `status`        VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/RUNNING/SUCCESS/FAILED',
  `progress`      INT          NOT NULL DEFAULT 0       COMMENT '进度百分比 0-100',
  `current_step`  VARCHAR(200) DEFAULT NULL              COMMENT '当前步骤描述',
  `password`      VARCHAR(255) DEFAULT NULL              COMMENT 'ZIP加密密码',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `finished_at`   DATETIME     DEFAULT NULL              COMMENT '完成时间',
  PRIMARY KEY (`id`),
  KEY `idx_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打包任务表';

-- ----------------------------------------
-- 初始数据：MinIO存储桶（需手动在MinIO中创建，此处仅做记录）
-- ----------------------------------------
-- project-files    项目文件
-- infra-components  基础组件
-- packages         打包产物
