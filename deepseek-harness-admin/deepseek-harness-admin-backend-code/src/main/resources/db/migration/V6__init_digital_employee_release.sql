-- 文件说明：V6 初始化数字员工发布版本表。
-- 兼容 MySQL 8.0 与测试库 H2（MySQL 模式）。
-- 发布记录保存不可变配置快照；回滚通过生成新的生效记录切换，不修改历史快照。

CREATE TABLE IF NOT EXISTS `digital_employee_release` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '发布记录主键',
    `digital_employee_id` BIGINT       NOT NULL COMMENT '数字员工 ID',
    `version`             VARCHAR(32)  NOT NULL COMMENT '发布版本号',
    `config_snapshot`     TEXT         NOT NULL COMMENT '配置快照（JSON，不可变）',
    `released_by`         BIGINT       NOT NULL COMMENT '发布人 ID，仅用于审计',
    `released_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `status`              VARCHAR(16)  NOT NULL COMMENT '发布状态：SUCCESS 或 FAILED',
    `failure_reason`      VARCHAR(500) NULL COMMENT '失败原因',
    PRIMARY KEY (`id`),
    KEY `idx_release_employee` (`digital_employee_id`)
);
