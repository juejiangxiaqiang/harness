-- 文件说明：V4 初始化 Skill 表与 Skill-知识库关联表。
-- 兼容 MySQL 8.0 与测试库 H2（MySQL 模式）。
-- skill_knowledge_base 设置联合唯一约束，防止同一知识库重复关联。

CREATE TABLE IF NOT EXISTS `skill` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Skill 主键',
    `name`        VARCHAR(128) NOT NULL COMMENT 'Skill 名称',
    `description` VARCHAR(500) NOT NULL COMMENT 'Skill 描述',
    `content`     TEXT         NOT NULL COMMENT 'Skill 配置或实现内容',
    `status`      VARCHAR(16)  NOT NULL COMMENT 'Skill 状态：ACTIVE 或 DISABLED',
    `version`     VARCHAR(32)  NOT NULL DEFAULT '1.0' COMMENT 'Skill 版本',
    `created_by`  BIGINT       NOT NULL COMMENT '创建管理员 ID，仅用于审计',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `skill_knowledge_base` (
    `id`                BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联主键',
    `skill_id`          BIGINT NOT NULL COMMENT 'Skill ID',
    `knowledge_base_id` BIGINT NOT NULL COMMENT '知识库 ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_skill_knowledge_base` (`skill_id`, `knowledge_base_id`)
);
