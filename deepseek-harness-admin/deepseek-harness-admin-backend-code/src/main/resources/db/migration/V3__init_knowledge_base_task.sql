-- 文件说明：V3 初始化知识库向量化任务表。
-- 兼容 MySQL 8.0 与测试库 H2（MySQL 模式）。
-- 幂等键唯一约束用于拦截并发重复任务；重新处理通过递增任务序号生成新幂等键。

CREATE TABLE IF NOT EXISTS `knowledge_base_task` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务主键',
    `knowledge_base_id` BIGINT       NOT NULL COMMENT '知识库 ID',
    `file_version`      VARCHAR(255) NOT NULL COMMENT '原始文件版本标识（存储键）',
    `idempotency_key`   VARCHAR(64)  NOT NULL COMMENT '幂等键，唯一',
    `status`            VARCHAR(16)  NOT NULL COMMENT '任务状态：PENDING/RUNNING/SUCCESS/FAILED',
    `retry_count`       INT          NOT NULL DEFAULT 0 COMMENT '已重试次数',
    `failure_reason`    VARCHAR(500) NULL COMMENT '最后失败原因',
    `started_at`        DATETIME     NULL COMMENT '开始时间',
    `finished_at`       DATETIME     NULL COMMENT '完成时间',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_idempotency_key` (`idempotency_key`),
    KEY `idx_task_kb_id` (`knowledge_base_id`)
);
