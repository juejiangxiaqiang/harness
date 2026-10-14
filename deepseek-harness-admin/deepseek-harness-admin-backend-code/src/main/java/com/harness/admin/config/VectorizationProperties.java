package com.harness.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件说明：向量化任务治理配置。
 *
 * <p>包括最大重试次数、指数退避基准与文本切分长度。
 * 重试为有限次数并带退避策略，避免失败任务无限重试。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Data
@Component
@ConfigurationProperties(prefix = "harness.vectorization")
public class VectorizationProperties {

    /** 最大重试次数（不含首次执行） */
    private int maxRetries = 2;

    /** 指数退避基准时长（秒） */
    private long backoffBaseSeconds = 2;

    /** 单个文档片段最大字符数 */
    private int chunkMaxChars = 800;

    /** 向量库标识（审计记录用，部署接入真实向量库时覆盖） */
    private String vectorStoreName = "simple-vector-store";

    /** 向量模型标识（审计记录用，部署接入真实模型时覆盖） */
    private String embeddingModelName = "local-hash";
}
