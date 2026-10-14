package com.harness.admin.domain.knowledgebase;

import java.time.LocalDateTime;

/**
 * 文件说明：知识库响应模型。
 *
 * <p>对外暴露知识库元数据与处理状态，不暴露存储路径等内部信息。</p>
 *
 * @param id             知识库 ID
 * @param name           知识库名称
 * @param status         处理状态
 * @param fileName       原始文件名
 * @param chunkCount     文档片段数量
 * @param vectorStore    向量数据库标识
 * @param embeddingModel 向量模型标识
 * @param failureReason  失败原因
 * @param createdAt      创建时间
 * @author harness-admin
 * @since 2026-10-06
 */
public record KnowledgeBaseResponse(
        Long id,
        String name,
        KnowledgeBaseStatus status,
        String fileName,
        int chunkCount,
        String vectorStore,
        String embeddingModel,
        String failureReason,
        LocalDateTime createdAt) {
}
