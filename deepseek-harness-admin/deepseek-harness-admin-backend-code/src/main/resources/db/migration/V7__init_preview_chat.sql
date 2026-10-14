-- 文件说明：V7 初始化管理端预览对话的会话表与消息表。
-- 兼容 MySQL 8.0 与测试库 H2（MySQL 模式）。
-- 对话历史落库持久化：会话（preview_conversation）与消息（preview_message），
-- 支撑管理员查看对话历史；问题与生成内容仅用于管理验证，不进入审计日志。

CREATE TABLE IF NOT EXISTS `preview_conversation` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话主键',
    `digital_employee_id` BIGINT       NOT NULL COMMENT '数字员工 ID（对话来源）',
    `title`               VARCHAR(64)  NULL COMMENT '会话标题（取首条问题摘要）',
    `created_by`          BIGINT       NOT NULL COMMENT '发起会话的管理员 ID，仅用于审计',
    `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近消息时间',
    PRIMARY KEY (`id`),
    KEY `idx_conversation_employee` (`digital_employee_id`)
);

CREATE TABLE IF NOT EXISTS `preview_message` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息主键',
    `conversation_id` BIGINT       NOT NULL COMMENT '所属会话 ID',
    `role`            VARCHAR(16)  NOT NULL COMMENT '消息角色：USER 或 ASSISTANT',
    `content`         TEXT         NOT NULL COMMENT '消息内容',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_message_conversation` (`conversation_id`)
);