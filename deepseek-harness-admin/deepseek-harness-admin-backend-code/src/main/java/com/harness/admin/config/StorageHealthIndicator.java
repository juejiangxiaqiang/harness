package com.harness.admin.config;

import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件说明：知识库文件存储健康指示器。
 *
 * <p>检查原始文件存储目录是否可写，纳入 actuator 健康检查；
 * 不返回连接凭据等敏感信息。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Component
public class StorageHealthIndicator extends AbstractHealthIndicator {

    /** 存储配置 */
    private final StorageProperties storageProperties;

    /**
     * 构造存储健康指示器。
     *
     * @param storageProperties 存储配置
     */
    public StorageHealthIndicator(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    /**
     * 评估存储目录可用性。
     *
     * @param builder 健康状态构建器
     * @throws Exception 检查过程异常
     */
    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        Path dir = Path.of(storageProperties.getKnowledgeBaseDir()).toAbsolutePath().normalize();
        if (Files.isDirectory(dir) && Files.isWritable(dir)) {
            builder.up().withDetail("knowledgeBaseDir", dir.toString());
        } else {
            builder.down().withDetail("knowledgeBaseDir", dir.toString());
        }
    }
}
