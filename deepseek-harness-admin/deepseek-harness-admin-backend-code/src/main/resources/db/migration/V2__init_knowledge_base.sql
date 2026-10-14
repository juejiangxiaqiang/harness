-- 文件说明：V2 初始化知识库表。
-- 兼容 MySQL 8.0 与测试库 H2（MySQL 模式）。
-- 知识库为全局共享资源，不设置 owner_id；created_by 仅用于审计。

CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '知识库主键',
    `name`            VARCHAR(128) NOT NULL COMMENT '知识库名称',
    `status`          VARCHAR(16)  NOT NULL COMMENT '处理状态：UPLOADING/PROCESSING/READY/FAILED',
    `file_name`       VARCHAR(255) NOT NULL COMMENT '原始文件名（规范化后）',
    `storage_key`     VARCHAR(255) NOT NULL COMMENT '文件存储标识（随机生成）',
    `chunk_count`     INT          NOT NULL DEFAULT 0 COMMENT '文档片段数量',
    `vector_store`    VARCHAR(64)  NULL COMMENT '向量数据库标识',
    `embedding_model` VARCHAR(64)  NULL COMMENT '向量模型标识',
    `failure_reason`  VARCHAR(500) NULL COMMENT '失败原因',
    `created_by`      BIGINT       NOT NULL COMMENT '创建管理员 ID，仅用于审计',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
);
