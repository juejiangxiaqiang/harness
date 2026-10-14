package com.harness.admin.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件说明：本地哈希向量模型（开发/测试用默认实现）。
 *
 * <p>根据文本摘要生成确定性向量，不依赖外部厂商服务，保证本地开发与测试可运行。
 * 通过 {@code @ConditionalOnMissingBean} 接入：引入真实向量模型 starter（如 OpenAI）
 * 后自动被替换，业务代码不绑定具体厂商。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
public class LocalHashEmbeddingModel implements EmbeddingModel {

    /** 向量维度 */
    private static final int DIMENSIONS = 16;

    /**
     * 对单个文档生成向量。
     *
     * @param document 文档
     * @return 向量
     */
    @Override
    public float[] embed(Document document) {
        return embed(document.getText());
    }

    /**
     * 对文本生成确定性向量（基于 SHA-256 摘要）。
     *
     * @param text 文本
     * @return 向量
     */
    @Override
    public float[] embed(String text) {
        byte[] hash = sha256(text);
        float[] vector = new float[DIMENSIONS];
        for (int i = 0; i < DIMENSIONS; i++) {
            // 摘要字节映射到 [-0.5, 0.5)，相同文本恒得相同向量
            vector[i] = ((hash[i] & 0xFF) / 255f) - 0.5f;
        }
        return vector;
    }

    /**
     * 调用嵌入请求，返回逐条向量。
     *
     * @param request 嵌入请求
     * @return 嵌入响应
     */
    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        List<String> instructions = request.getInstructions();
        for (int i = 0; i < instructions.size(); i++) {
            embeddings.add(new Embedding(embed(instructions.get(i)), i));
        }
        return new EmbeddingResponse(embeddings);
    }

    /**
     * 返回向量维度。
     *
     * @return 维度
     */
    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    /**
     * 计算 SHA-256 摘要。
     *
     * @param text 文本
     * @return 摘要字节
     */
    private byte[] sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            // JDK 17 必然支持 SHA-256，此处仅防御性兜底
            throw new IllegalStateException("SHA-256 算法不可用", ex);
        }
    }
}
