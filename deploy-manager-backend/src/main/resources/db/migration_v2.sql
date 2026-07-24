-- ============================================
-- DeployManager V2 数据库迁移脚本
-- Phase 1: 用户认证 + License + 项目/文件表扩展
-- ============================================

USE `deploy_manager`;

-- ----------------------------------------
-- 1. 用户表
-- ----------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
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
-- 2. License 记录表
-- ----------------------------------------
CREATE TABLE IF NOT EXISTS `license_record` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id`    BIGINT       DEFAULT NULL              COMMENT '所属项目ID',
  `customer_name` VARCHAR(200) DEFAULT NULL              COMMENT '客户名称',
  `license_file`  VARCHAR(500) DEFAULT NULL              COMMENT 'License文件路径',
  `expire_date`   DATETIME     DEFAULT NULL              COMMENT '到期日期',
  `trial_days`    INT          DEFAULT NULL              COMMENT '试用天数',
  `type`          VARCHAR(20)  NOT NULL DEFAULT 'TRIAL'  COMMENT '类型: TRIAL/OFFICIAL/RENEWAL',
  `status`        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/EXPIRED/REVOKED',
  `parent_id`     BIGINT       DEFAULT NULL              COMMENT '父License ID（续期场景）',
  `generated_by`  BIGINT       DEFAULT NULL              COMMENT '生成人用户ID',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_project` (`project_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='License记录表';

-- ----------------------------------------
-- 3. 扩展 uploaded_file 表
-- ----------------------------------------
ALTER TABLE `uploaded_file`
  ADD COLUMN IF NOT EXISTS `version`       VARCHAR(50)  DEFAULT NULL COMMENT '版本号',
  ADD COLUMN IF NOT EXISTS `version_label` VARCHAR(50)  DEFAULT NULL COMMENT '版本标签',
  ADD COLUMN IF NOT EXISTS `init_state`    VARCHAR(20)  DEFAULT NULL COMMENT '初始化状态',
  ADD COLUMN IF NOT EXISTS `belong_system` VARCHAR(50)  DEFAULT NULL COMMENT '所属系统';

-- ----------------------------------------
-- 4. 扩展 project 表
-- ----------------------------------------
ALTER TABLE `project`
  ADD COLUMN IF NOT EXISTS `redis_component_id` BIGINT       DEFAULT NULL     COMMENT 'Redis组件ID',
  ADD COLUMN IF NOT EXISTS `redis_port`         INT          DEFAULT 6379    COMMENT 'Redis端口',
  ADD COLUMN IF NOT EXISTS `include_redis`      TINYINT(1)   DEFAULT 0      COMMENT '是否包含Redis: 0否 1是';

-- 删除 sql_file_id 列（已废弃，改用数据库配置字段）
ALTER TABLE `project` DROP COLUMN IF EXISTS `sql_file_id`;

-- ----------------------------------------
-- 5. 初始化默认管理员用户
-- 密码: admin123 (BCrypt加密)
-- ----------------------------------------
INSERT INTO `user` (`username`, `password`, `display_name`, `role`)
VALUES ('admin', '{bcrypt}$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '管理员', 'ADMIN')
ON DUPLICATE KEY UPDATE `username` = `username`;
