-- 文件说明：V5 初始化数字员工表与数字员工-Skill 关联表。
-- 兼容 MySQL 8.0 与测试库 H2（MySQL 模式）。
-- 数字员工不直接关联知识库；其知识库集合由关联 Skills 计算。
-- digital_employee_skill 设置联合唯一约束，防止重复关联。

CREATE TABLE IF NOT EXISTS `digital_employee` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '数字员工主键',
    `name`              VARCHAR(128) NOT NULL COMMENT '名称',
    `description`       VARCHAR(500) NOT NULL COMMENT '描述',
    `system_prompt`     TEXT         NOT NULL COMMENT '系统提示词',
    `model_config`      TEXT         NOT NULL COMMENT '模型配置（JSON）',
    `status`            VARCHAR(16)  NOT NULL COMMENT '状态：DRAFT/PUBLISHED/ARCHIVED',
    `published_version` VARCHAR(32)  NULL COMMENT '当前发布版本',
    `created_by`        BIGINT       NOT NULL COMMENT '创建管理员 ID，仅用于审计',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `digital_employee_skill` (
    `id`                  BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联主键',
    `digital_employee_id` BIGINT NOT NULL COMMENT '数字员工 ID',
    `skill_id`            BIGINT NOT NULL COMMENT 'Skill ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_employee_skill` (`digital_employee_id`, `skill_id`)
);
