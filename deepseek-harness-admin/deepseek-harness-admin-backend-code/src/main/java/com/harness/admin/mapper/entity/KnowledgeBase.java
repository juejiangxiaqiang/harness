package com.harness.admin.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.harness.admin.domain.knowledgebase.KnowledgeBaseStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件说明：知识库持久化实体。
 *
 * <p>对应 {@code knowledge_base} 表。知识库为全局共享资源，不设置 owner_id；
 * created_by 仅用于审计，不作为资源所有权与读取过滤条件。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Data
@TableName("`knowledge_base`")
public class KnowledgeBase {

    /** 知识库主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 知识库名称 */
    private String name;

    /** 处理状态 */
    private KnowledgeBaseStatus status;

    /** 原始文件名（规范化后） */
    private String fileName;

    /** 文件存储标识（随机生成） */
    private String storageKey;

    /** 文档片段数量 */
    private Integer chunkCount;

    /** 向量数据库标识 */
    private String vectorStore;

    /** 向量模型标识 */
    private String embeddingModel;

    /** 失败原因 */
    private String failureReason;

    /** 创建管理员 ID，仅用于审计 */
    private Long createdBy;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
