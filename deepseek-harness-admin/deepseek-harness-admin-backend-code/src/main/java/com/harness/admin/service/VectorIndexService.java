package com.harness.admin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件说明：向量索引服务。
 *
 * <p>将文档片段封装为带稳定 ID 与元数据的 {@link Document} 写入向量库；
 * 稳定 ID 保证重试写入为幂等 upsert。按知识库维度删除向量，支撑重新处理前清理旧向量。
 * 具体向量库由 {@link VectorStore} 抽象决定，业务不绑定厂商。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Component
public class VectorIndexService {

    /** 结构化日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(VectorIndexService.class);

    /** 知识库 ID 元数据键（小写下划线，兼容 Redis 搜索字段命名） */
    public static final String METADATA_KB_ID = "kb_id";

    /** 文件版本元数据键（小写下划线，兼容 Redis 搜索字段命名） */
    public static final String METADATA_FILE_VERSION = "file_version";

    /** 向量存储 */
    private final VectorStore vectorStore;

    /**
     * 构造向量索引服务。
     *
     * @param vectorStore 向量存储（Spring AI 抽象）
     */
    public VectorIndexService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 写入文档片段向量（稳定片段 ID，重试幂等）。
     *
     * @param kbId        知识库 ID
     * @param storageKey  文件存储键
     * @param chunks      文档片段
     * @return 写入片段数
     */
    public int write(Long kbId, String storageKey, List<String> chunks) {
        List<Document> documents = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put(METADATA_KB_ID, kbId);
            metadata.put(METADATA_FILE_VERSION, storageKey);
            // 片段 ID 由知识库、文件版本与序号确定，同参数重复写入覆盖同 ID 向量
            documents.add(new Document(chunkId(kbId, storageKey, i), chunks.get(i), metadata));
        }
        vectorStore.add(documents);
        return documents.size();
    }

    /**
     * 删除知识库的全部向量（重新处理前清理旧版本向量）。
     *
     * <p>best-effort：向量库首次写入前元数据字段尚未注册、或索引尚不存在时无法按过滤删除，
     * 此时无可清理内容，失败不阻塞向量化。ponytail: 该路径可能掩盖真实删除失败，
     * 后续可改为按向量库索引存在性判断是否执行清理。</p>
     *
     * @param kbId 知识库 ID
     */
    public void deleteByKnowledgeBase(Long kbId) {
        try {
            vectorStore.delete(new FilterExpressionBuilder().eq(METADATA_KB_ID, kbId).build());
        } catch (RuntimeException ex) {
            log.warn("skip vector cleanup, delete by kb filter failed, kbId={}, detail={}",
                    kbId, ex.getMessage());
        }
    }

    /**
     * 生成稳定的片段 ID。
     *
     * @param kbId       知识库 ID
     * @param storageKey 文件存储键
     * @param index      片段序号
     * @return 片段 ID
     */
    public String chunkId(Long kbId, String storageKey, int index) {
        return "kb-" + kbId + "-" + storageKey + "-" + index;
    }
}
