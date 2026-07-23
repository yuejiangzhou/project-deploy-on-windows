-- ============================================
-- 数据库迁移脚本（幂等版本）
-- 所有 ALTER TABLE 通过 INFORMATION_SCHEMA 判断避免重复执行
-- 执行前请先备份数据库
-- ============================================

USE `deploy_manager`;

-- ============================================
-- uploaded_file 表增量字段
-- ============================================

DROP PROCEDURE IF EXISTS `add_column_if_not_exists`;
DELIMITER ;;
CREATE PROCEDURE `add_column_if_not_exists`(
    IN p_table_name VARCHAR(100),
    IN p_column_name VARCHAR(100),
    IN p_column_type VARCHAR(200),
    IN p_column_comment VARCHAR(255)
)
BEGIN
    DECLARE col_count INT;
    SELECT COUNT(*) INTO col_count
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'deploy_manager'
      AND TABLE_NAME = p_table_name
      AND COLUMN_NAME = p_column_name;
    IF col_count = 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN `', p_column_name, '` ', p_column_type, ' COMMENT ''', p_column_comment, '''');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END;;
DELIMITER ;

-- uploaded_file 表字段
CALL add_column_if_not_exists('uploaded_file', 'version', 'VARCHAR(100) DEFAULT NULL', '版本号');
CALL add_column_if_not_exists('uploaded_file', 'tag', 'VARCHAR(100) DEFAULT NULL', '标签');
CALL add_column_if_not_exists('uploaded_file', 'init_state', 'VARCHAR(20) DEFAULT NULL', '初始化状态: clean纯净版/initialized已初始化');
CALL add_column_if_not_exists('uploaded_file', 'belongs_to', 'VARCHAR(100) DEFAULT NULL', '所属系统');

-- project 表增量字段（从旧版升级时使用）
CALL add_column_if_not_exists('project', 'nginx_conf_content', 'TEXT DEFAULT NULL', 'Nginx配置文件内容');
CALL add_column_if_not_exists('project', 'jdk_version', 'VARCHAR(50) DEFAULT NULL', 'JDK版本');
CALL add_column_if_not_exists('project', 'mysql_version', 'VARCHAR(50) DEFAULT NULL', 'MySQL版本');
CALL add_column_if_not_exists('project', 'mysql_config_state', 'VARCHAR(20) DEFAULT ''clean''', 'MySQL配置状态: clean纯净版/configured已初始化');
CALL add_column_if_not_exists('project', 'minio_version', 'VARCHAR(50) DEFAULT NULL', 'MinIO版本');
CALL add_column_if_not_exists('project', 'minio_config_state', 'VARCHAR(20) DEFAULT ''clean''', 'MinIO配置状态: clean纯净版/configured已初始化');
CALL add_column_if_not_exists('project', 'nginx_version', 'VARCHAR(50) DEFAULT NULL', 'Nginx版本');
CALL add_column_if_not_exists('project', 'engine_version', 'VARCHAR(50) DEFAULT NULL', '引擎版本');
CALL add_column_if_not_exists('project', 'db_name', 'VARCHAR(100) DEFAULT NULL', '数据库名');
CALL add_column_if_not_exists('project', 'db_username', 'VARCHAR(50) DEFAULT NULL', '数据库用户名');
CALL add_column_if_not_exists('project', 'db_password', 'VARCHAR(100) DEFAULT NULL', '数据库密码');
CALL add_column_if_not_exists('project', 'minio_access_key', 'VARCHAR(100) DEFAULT NULL', 'MinIO AccessKey');
CALL add_column_if_not_exists('project', 'minio_secret_key', 'VARCHAR(100) DEFAULT NULL', 'MinIO SecretKey');

-- 清理存储过程
DROP PROCEDURE IF EXISTS `add_column_if_not_exists`;

-- ============================================
-- 操作日志表（新增，幂等）
-- ============================================

CREATE TABLE IF NOT EXISTS `operation_log` (
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

-- 验证
SELECT 'Migration completed successfully' AS result;