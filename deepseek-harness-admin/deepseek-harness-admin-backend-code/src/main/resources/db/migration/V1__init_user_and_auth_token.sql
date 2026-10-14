-- 文件说明：V1 初始化用户表与认证凭证表。
-- 兼容 MySQL 8.0 与测试库 H2（MySQL 模式），不使用 ENGINE/COMMENT 等非通用语法。

-- 用户表：登录账号唯一，角色仅由服务端受控流程授予
CREATE TABLE IF NOT EXISTS `user` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户主键',
    `username`      VARCHAR(64)  NOT NULL COMMENT '登录账号，唯一',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希（BCrypt）',
    `role`          VARCHAR(16)  NOT NULL COMMENT '用户角色：ADMIN 或 USER',
    `status`        VARCHAR(16)  NOT NULL COMMENT '用户状态：ACTIVE 或 DISABLED',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`)
);

-- 认证凭证表：凭证仅存哈希，支持过期与吊销（退出登录）
CREATE TABLE IF NOT EXISTS `user_auth_token` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '凭证主键',
    `user_id`    BIGINT      NOT NULL COMMENT '用户 ID',
    `token_hash` VARCHAR(64) NOT NULL COMMENT '凭证哈希（SHA-256）',
    `expires_at` DATETIME    NOT NULL COMMENT '过期时间',
    `revoked`    BOOLEAN     NOT NULL DEFAULT FALSE COMMENT '是否已吊销',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_token_hash` (`token_hash`),
    KEY `idx_token_user_id` (`user_id`)
);
