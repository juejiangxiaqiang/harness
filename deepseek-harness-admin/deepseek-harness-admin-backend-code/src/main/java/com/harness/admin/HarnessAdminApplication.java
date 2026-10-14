package com.harness.admin;

import org.springframework.ai.vectorstore.redis.autoconfigure.RedisVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <p>last_updated: 2026-10-08</p>
 * <p>status: active          # active | deprecated | draft</p>
 * <p>owner: @qiang</p>
 *
 * <p>文件说明：Harness 管理后台后端服务启动入口。</p>
 *
 * <p>面向管理员维护全局共享的知识库、Skills 和数字员工，并发布数字员工版本。
 * 分层依赖方向：domain -&gt; config -&gt; mapper -&gt; service -&gt; controller。</p>
 *
 * <p>排除 Redis {@code RedisVectorStoreAutoConfiguration}：默认自动配置的 RedisVectorStore
 * 不会把 {@code kb_id}/{@code file_version} 注册进索引 schema，导致按知识库检索失效；
 * 改用 {@code SpringAiConfig#redisVectorStore} 显式携带 {@code metadataFields} 构建（见该注解）。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@SpringBootApplication(exclude = RedisVectorStoreAutoConfiguration.class)
public class HarnessAdminApplication {

    /**
     * 启动管理后台服务。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(HarnessAdminApplication.class, args);
    }
}
