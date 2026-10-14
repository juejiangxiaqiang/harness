package com.harness.admin.service;

import com.harness.admin.config.VectorizationProperties;
import com.harness.admin.domain.knowledgebase.KnowledgeBaseStatus;
import com.harness.admin.domain.knowledgebase.KnowledgeBaseTaskStatus;
import com.harness.admin.mapper.KnowledgeBaseMapper;
import com.harness.admin.mapper.KnowledgeBaseTaskMapper;
import com.harness.admin.mapper.entity.KnowledgeBase;
import com.harness.admin.mapper.entity.KnowledgeBaseTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

/**
 * 文件说明：向量化任务状态持久化服务。
 *
 * <p>负责任务与知识库状态的双表原子流转（执行中/成功/最终失败），
 * 从任务治理服务拆分，保证单文件规模与单一职责。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Component
public class VectorizationTaskStateService {

    /** 结构化日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(VectorizationTaskStateService.class);

    /** 任务表数据访问 */
    private final KnowledgeBaseTaskMapper taskMapper;

    /** 知识库表数据访问 */
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    /** 编程式事务模板（异步线程内多表状态更新） */
    private final TransactionTemplate transactionTemplate;

    /** 向量化配置（向量库/模型审计标识） */
    private final VectorizationProperties vectorizationProperties;

    /**
     * 构造状态持久化服务。
     *
     * @param taskMapper               任务表数据访问
     * @param knowledgeBaseMapper      知识库表数据访问
     * @param transactionTemplate      事务模板
     * @param vectorizationProperties  向量化配置
     */
    public VectorizationTaskStateService(KnowledgeBaseTaskMapper taskMapper, KnowledgeBaseMapper knowledgeBaseMapper,
                                         TransactionTemplate transactionTemplate,
                                         VectorizationProperties vectorizationProperties) {
        this.taskMapper = taskMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.transactionTemplate = transactionTemplate;
        this.vectorizationProperties = vectorizationProperties;
    }

    /**
     * 标记任务执行中，并将知识库状态置为 PROCESSING。
     *
     * @param task 任务
     */
    public void markRunning(KnowledgeBaseTask task) {
        transactionTemplate.executeWithoutResult(status -> {
            task.setStatus(KnowledgeBaseTaskStatus.RUNNING);
            task.setStartedAt(LocalDateTime.now());
            task.setFailureReason(null);
            taskMapper.updateById(task);
            KnowledgeBase kb = knowledgeBaseMapper.selectById(task.getKnowledgeBaseId());
            if (kb != null) {
                kb.setStatus(KnowledgeBaseStatus.PROCESSING);
                kb.setFailureReason(null);
                knowledgeBaseMapper.updateById(kb);
            }
        });
    }

    /**
     * 标记任务成功，并将知识库置为 READY（向量已完整写入）。
     *
     * @param task       任务
     * @param kb         知识库
     * @param chunkCount 片段数
     */
    public void markSuccess(KnowledgeBaseTask task, KnowledgeBase kb, int chunkCount) {
        transactionTemplate.executeWithoutResult(status -> {
            task.setStatus(KnowledgeBaseTaskStatus.SUCCESS);
            task.setFinishedAt(LocalDateTime.now());
            task.setFailureReason(null);
            taskMapper.updateById(task);
            kb.setStatus(KnowledgeBaseStatus.READY);
            kb.setChunkCount(chunkCount);
            kb.setVectorStore(vectorizationProperties.getVectorStoreName());
            kb.setEmbeddingModel(vectorizationProperties.getEmbeddingModelName());
            kb.setFailureReason(null);
            knowledgeBaseMapper.updateById(kb);
        });
        log.info("knowledge base vectorized, kbId={}, chunkCount={}, taskId={}",
                kb.getId(), chunkCount, task.getId());
    }

    /**
     * 标记任务最终失败，并将知识库置为 FAILED。
     *
     * @param task   任务
     * @param reason 失败原因
     */
    public void markFailed(KnowledgeBaseTask task, String reason) {
        transactionTemplate.executeWithoutResult(status -> {
            task.setStatus(KnowledgeBaseTaskStatus.FAILED);
            task.setFinishedAt(LocalDateTime.now());
            task.setFailureReason(reason);
            taskMapper.updateById(task);
            KnowledgeBase kb = knowledgeBaseMapper.selectById(task.getKnowledgeBaseId());
            if (kb != null) {
                kb.setStatus(KnowledgeBaseStatus.FAILED);
                kb.setFailureReason(reason);
                knowledgeBaseMapper.updateById(kb);
            }
        });
        log.error("vectorization final failed, taskId={}, kbId={}, reason={}",
                task.getId(), task.getKnowledgeBaseId(), reason);
    }
}
