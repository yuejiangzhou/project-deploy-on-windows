-- ============================================
-- DeployManager 数据库建表脚本（完整最新版 V2）
-- 数据库: MySQL 8.0
-- 字符集: utf8
-- ============================================

CREATE DATABASE IF NOT EXISTS `deploy_manager`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE `deploy_manager`;

-- ----------------------------------------
-- 1. 用户表
-- ----------------------------------------
CREATE TABLE `user` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`     VARCHAR(50)  NOT NULL                COMMENT '用户名',
  `password`     VARCHAR(200) NOT NULL                COMMENT '密码（BCrypt加密）',
  `display_name` VARCHAR(100) DEFAULT NULL              COMMENT '显示名称',
  `role`         VARCHAR(20)  NOT NULL DEFAULT 'DEVELOPER' COMMENT '角色: ADMIN/DEVELOPER',
  `status`       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'  COMMENT '状态: ACTIVE/DISABLED',
  `last_login`   DATETIME     DEFAULT NULL              COMMENT '最后登录时间',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------------------
-- 2. 项目表
-- ----------------------------------------
CREATE TABLE `project` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`                VARCHAR(100) NOT NULL                COMMENT '项目名称',
  `description`         VARCHAR(500) DEFAULT NULL              COMMENT '项目描述',
  `status`              VARCHAR(20)  NOT NULL DEFAULT 'CONFIGURING' COMMENT '状态: CONFIGURING/READY/ARCHIVED',
  `jar_file_id`         BIGINT       DEFAULT NULL              COMMENT '选中的JAR包文件ID',
  `vue_folder_id`       BIGINT       DEFAULT NULL              COMMENT '选中的Vue产物文件夹ID',
  `nginx_conf_file_id`  BIGINT       DEFAULT NULL              COMMENT '选中的Nginx配置文件ID(预留)',
  `nginx_conf_content`  TEXT         DEFAULT NULL              COMMENT 'Nginx配置文件内容',
  `app_port`            INT          NOT NULL DEFAULT 8080     COMMENT '应用端口',
  `jvm_params`          VARCHAR(500) NOT NULL DEFAULT '-Xms512m -Xmx2048m' COMMENT 'JVM参数',
  `mysql_port`          INT          NOT NULL DEFAULT 3306     COMMENT 'MySQL端口',
  `db_name`             VARCHAR(100) DEFAULT NULL              COMMENT '数据库名',
  `db_username`         VARCHAR(50)  DEFAULT NULL              COMMENT '数据库用户名',
  `db_password`         VARCHAR(100) DEFAULT NULL              COMMENT '数据库密码',
  `minio_api_port`      INT          NOT NULL DEFAULT 9000     COMMENT 'MinIO API端口',
  `minio_console_port`  INT          NOT NULL DEFAULT 9001     COMMENT 'MinIO Console端口',
  `minio_access_key`    VARCHAR(100) DEFAULT NULL              COMMENT 'MinIO AccessKey',
  `minio_secret_key`    VARCHAR(100) DEFAULT NULL              COMMENT 'MinIO SecretKey',
  `nginx_http_port`     INT          NOT NULL DEFAULT 80       COMMENT 'Nginx HTTP端口',
  `engine_enabled`      TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '是否包含引擎: 0否 1是',
  `engine_port`         INT          NOT NULL DEFAULT 8090     COMMENT '引擎端口',
  `jdk_component_id`    BIGINT       DEFAULT NULL              COMMENT '选定的JDK组件ID',
  `mysql_component_id`  BIGINT       DEFAULT NULL              COMMENT '选定的MySQL组件ID',
  `minio_component_id`  BIGINT       DEFAULT NULL              COMMENT '选定的MinIO组件ID',
  `nginx_component_id`  BIGINT       DEFAULT NULL              COMMENT '选定的Nginx组件ID',
  `engine_component_id` BIGINT       DEFAULT NULL              COMMENT '选定的引擎组件ID',
  `redis_component_id`  BIGINT       DEFAULT NULL              COMMENT '选定的Redis组件ID',
  `include_jdk`         TINYINT(1)   NOT NULL DEFAULT 1       COMMENT '是否包含JDK: 0否 1是',
  `include_mysql`       TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '是否包含MySQL: 0否 1是',
  `include_minio`       TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '是否包含MinIO: 0否 1是',
  `include_nginx`       TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '是否包含Nginx: 0否 1是',
  `include_redis`       TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '是否包含Redis: 0否 1是',
  `jdk_version`         VARCHAR(50)  DEFAULT NULL              COMMENT 'JDK版本',
  `mysql_version`       VARCHAR(50)  DEFAULT NULL              COMMENT 'MySQL版本',
  `mysql_config_state`  VARCHAR(20)  DEFAULT 'clean'           COMMENT 'MySQL配置状态: clean纯净版/configured已初始化',
  `minio_version`       VARCHAR(50)  DEFAULT NULL              COMMENT 'MinIO版本',
  `minio_config_state`  VARCHAR(20)  DEFAULT 'clean'           COMMENT 'MinIO配置状态: clean纯净版/configured已初始化',
  `nginx_version`       VARCHAR(50)  DEFAULT NULL              COMMENT 'Nginx版本',
  `engine_version`      VARCHAR(50)  DEFAULT NULL              COMMENT '引擎版本',
  `redis_port`          INT          DEFAULT 6379              COMMENT 'Redis端口',
  `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`             TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- ----------------------------------------
-- 3. 上传文件表
-- ----------------------------------------
CREATE TABLE `uploaded_file` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id`      BIGINT       DEFAULT NULL              COMMENT '所属项目ID(基础组件为空)',
  `file_type`       VARCHAR(20)  NOT NULL                COMMENT '文件类型: jar/vue/jdk/mysql/minio/nginx/engine',
  `file_name`       VARCHAR(255) NOT NULL                COMMENT '文件/文件夹名称',
  `file_size`       BIGINT       DEFAULT NULL              COMMENT '文件大小(字节)',
  `minio_bucket`    VARCHAR(100) DEFAULT NULL              COMMENT 'MinIO存储桶名',
  `minio_object_key` VARCHAR(500) DEFAULT NULL              COMMENT 'MinIO对象键',
  `is_folder`       TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '是否为文件夹: 0否 1是',
  `folder_path`     VARCHAR(500) DEFAULT NULL              COMMENT '文件夹路径，如 ecom/jar/2026-06-28',
  `upload_date`     DATE         DEFAULT NULL              COMMENT '上传日期(用于三级目录的日期级别)',
  `version_tag`     VARCHAR(100) DEFAULT NULL              COMMENT '版本标签',
  `version`         VARCHAR(50)  DEFAULT NULL              COMMENT '版本号',
  `version_label`   VARCHAR(50)  DEFAULT NULL              COMMENT '版本标签',
  `tag`             VARCHAR(100) DEFAULT NULL              COMMENT '标签',
  `init_state`      VARCHAR(20)  DEFAULT NULL              COMMENT '初始化状态: clean纯净版/initialized已初始化',
  `belongs_to`      VARCHAR(100) DEFAULT NULL              COMMENT '所属系统（兼容旧字段）',
  `belong_system`   VARCHAR(50)  DEFAULT NULL              COMMENT '所属系统',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `deleted`         TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_project_type` (`project_id`, `file_type`),
  KEY `idx_folder_path` (`folder_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上传文件表';

-- ----------------------------------------
-- 4. 打包产物表
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
-- 5. 打包任务表（用于进度追踪）
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
-- 6. 操作日志表
-- ----------------------------------------
CREATE TABLE `operation_log` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '日志ID',
  `module`        VARCHAR(50)  NOT NULL                  COMMENT '模块: PROJECT/FILE/INFRA/PACKAGE',
  `action`        VARCHAR(50)  NOT NULL                  COMMENT '操作: CREATE/UPDATE/DELETE/UPLOAD/DOWNLOAD/START',
  `target_type`   VARCHAR(50)  DEFAULT NULL              COMMENT '目标类型: PROJECT/FILE/COMPONENT/PACKAGE',
  `target_id`     BIGINT       DEFAULT NULL              COMMENT '目标ID',
  `target_name`   VARCHAR(200) DEFAULT NULL              COMMENT '目标名称',
  `detail`        TEXT         DEFAULT NULL              COMMENT '操作详情',
  `status`        VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS' COMMENT '状态: SUCCESS/FAILED',
  `operator`      VARCHAR(100) DEFAULT 'system'          COMMENT '操作人',
  `ip`            VARCHAR(50)  DEFAULT NULL              COMMENT '操作IP',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_module` (`module`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ----------------------------------------
-- 7. License 记录表
-- ----------------------------------------
CREATE TABLE `license_record` (
  `id`                       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id`               BIGINT       DEFAULT NULL              COMMENT '所属项目ID',
  `license_id`               VARCHAR(50)  DEFAULT NULL              COMMENT 'License编号(UUID)',
  `type`                     VARCHAR(20)  NOT NULL DEFAULT 'TRIAL'  COMMENT '类型: TRIAL/OFFICIAL/RENEWAL',
  `customer_name`            VARCHAR(200) DEFAULT NULL              COMMENT '客户名称',
  `issue_date`               DATE         DEFAULT NULL              COMMENT '签发日期',
  `expire_date`              DATE         DEFAULT NULL              COMMENT '到期日期',
  `trial_days`               INT          DEFAULT NULL              COMMENT '试用天数',
  `data_json`                TEXT         DEFAULT NULL              COMMENT 'License数据JSON',
  `signature`                TEXT         DEFAULT NULL              COMMENT 'RSA签名(Base64)',
  `minio_bucket`             VARCHAR(100) DEFAULT NULL              COMMENT 'MinIO存储桶',
  `minio_lic_object_key`     VARCHAR(500) DEFAULT NULL              COMMENT 'MinIO .lic文件对象键',
  `minio_timestamp_object_key` VARCHAR(500) DEFAULT NULL            COMMENT 'MinIO timestamp.dat对象键',
  `parent_id`                BIGINT       DEFAULT NULL              COMMENT '父License ID（续期场景）',
  `generated_by`             BIGINT       DEFAULT NULL              COMMENT '生成人用户ID',
  `status`                   VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/EXPIRED/REVOKED/RENEWED',
  `status_note`              VARCHAR(500) DEFAULT NULL              COMMENT '状态备注',
  `deleted`                  TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0否 1是',
  `created_at`               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_project` (`project_id`),
  KEY `idx_status` (`status`),
  KEY `idx_license_id` (`license_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='License记录表';

-- ----------------------------------------
-- 8. 初始化默认管理员用户
-- 密码: admin123 (BCrypt加密)
-- ----------------------------------------
INSERT INTO `user` (`username`, `password`, `display_name`, `role`)
VALUES ('admin', '{bcrypt}$2a$10$228/ty3ZX3ZpOA9CXBObfOjyZUN9kSXZEjULMrv4jtnwhMBZfyHPm', '管理员', 'ADMIN')
ON DUPLICATE KEY UPDATE `username` = `username`;
