package com.harness.admin.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.harness.admin.domain.knowledgebase.KnowledgeBaseTaskStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件说明：知识库向量化任务持久化实体。
 *
 * <p>对应 {@code knowledge_base_task} 表。每次向量化对应唯一任务记录，
 * 幂等键防止并发重复任务，重试次数与失败原因支撑任务治理。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Data
@TableName("`knowledge_base_task`")
public class KnowledgeBaseTask {

    /** 任务主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 知识库 ID */
    private Long knowledgeBaseId;

    /** 原始文件版本标识（存储键） */
    private String fileVersion;

    /** 幂等键 */
    private String idempotencyKey;

    /** 任务状态 */
    private KnowledgeBaseTaskStatus status;

    /** 已重试次数 */
    private Integer retryCount;

    /** 最后失败原因 */
    private String failureReason;

    /** 开始时间 */
    private LocalDateTime startedAt;

    /** 完成时间 */
    private LocalDateTime finishedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
