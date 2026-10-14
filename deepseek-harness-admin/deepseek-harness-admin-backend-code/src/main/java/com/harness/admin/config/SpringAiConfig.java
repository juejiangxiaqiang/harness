package com.harness.admin.config;

import com.harness.admin.service.VectorIndexService;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore.MetadataField;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.search.Schema.FieldType;

// ---
// last_updated: 2026-10-08
// status: active
// owner: @qiang
// ---
/**
 * 文件说明：Spring AI 接入配置。
 *
 * <p>向量模型与向量数据库通过 Spring AI 抽象接入，业务代码不绑定具体厂商。
 * 缺省提供本地哈希模型与内存向量库，保证无外部依赖可运行；
 * 部署时引入真实厂商 starter 后自动替换。</p>
 *
 * <p>约束：Redis 向量库的 schema 需注册 {@code kb_id}/{@code file_version} 元数据字段，
 * 否则按知识库过滤（filterExpression）与召回元数据均为空，导致对话检索失效；
 * 因此生产屏蔽默认 RedisVectorStore 自动配置，显式带 {@code metadataFields} 构建。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Configuration
public class SpringAiConfig {

    /**
     * 本地哈希向量模型：仅当 {@code spring.ai.model.embedding=local}（或缺省）时创建，
     * 避免与厂商 EmbeddingModel 同时注册造成注入歧义。
     *
     * @return 向量模型
     */
    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    @ConditionalOnProperty(prefix = "spring.ai.model", name = "embedding", havingValue = "local", matchIfMissing = true)
    public EmbeddingModel localHashEmbeddingModel() {
        return new LocalHashEmbeddingModel();
    }

    /**
     * 内存向量库：仅当 {@code spring.ai.vectorstore.type=local}（或缺省）时创建，
     * 避免抢先注册导致 Redis 等厂商向量库自动配置（@ConditionalOnMissingBean）失效。
     *
     * @param embeddingModel 向量模型
     * @return 向量存储
     */
    @Bean
    @ConditionalOnMissingBean(VectorStore.class)
    @ConditionalOnProperty(prefix = "spring.ai.vectorstore", name = "type", havingValue = "local", matchIfMissing = true)
    public VectorStore simpleVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * Redis 向量库：仅当 {@code spring.ai.vectorstore.type=redis} 时创建。
     *
     * <p>意图：默认 RedisVectorStore 自动配置不会把业务元数据（{@code kb_id}、{@code file_version}）
     * 注册进 RediSearch schema，导致「按知识库过滤」与「召回元数据」均为空，对话检索失效。
     * 此处显式构建并声明 {@code metadataFields}，使 schema 在自动创建时即含这两字段，
     * 与 {@link PreviewChatService} 的按库分段检索（filterExpression on kb_id）配套。
     * 同时应用入口已排除 {@code RedisVectorStoreAutoConfiguration}，确保本 bean 是唯一候选。</p>
     *
     * <p>约束：配置项通过 {@code @Value} 自取，不依赖已排除的自动配置 properties；
     * 存量索引需执行一次 {@code FT.ALTER ... SCHEMA ADD $.kb_id AS kb_id NUMERIC}
     * 才能按 kb_id 过滤（见 docs/DEPLOYMENT.md）。</p>
     *
     * @param embeddingModel         向量模型
     * @param jedisConnectionFactory Jedis 连接工厂（主机/端口/密码来自 spring.data.redis）
     * @return 带元数据字段的 Redis 向量存储
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.ai.vectorstore", name = "type", havingValue = "redis")
    public VectorStore redisVectorStore(
            @Value("${spring.ai.vectorstore.redis.index-name:harness_kb_index}") String indexName,
            @Value("${spring.ai.vectorstore.redis.prefix:kb:}") String prefix,
            @Value("${spring.ai.vectorstore.redis.initialize-schema:true}") boolean initializeSchema,
            EmbeddingModel embeddingModel, JedisConnectionFactory jedisConnectionFactory) {
        JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
                .ssl(jedisConnectionFactory.isUseSsl())
                .clientName(jedisConnectionFactory.getClientName())
                .timeoutMillis(jedisConnectionFactory.getTimeout())
                .password(jedisConnectionFactory.getPassword())
                .build();
        RedisClient jedisClient = RedisClient.builder()
                .hostAndPort(jedisConnectionFactory.getHostName(), jedisConnectionFactory.getPort())
                .clientConfig(clientConfig)
                .build();
        return RedisVectorStore.builder(jedisClient, embeddingModel)
                .initializeSchema(initializeSchema)
                .indexName(indexName)
                .prefix(prefix)
                .metadataFields(
                        new MetadataField(VectorIndexService.METADATA_KB_ID, FieldType.NUMERIC),
                        new MetadataField(VectorIndexService.METADATA_FILE_VERSION, FieldType.TEXT))
                .hnswM(16)
                .hnswEfConstruction(200)
                .hnswEfRuntime(10)
                .build();
    }
}
