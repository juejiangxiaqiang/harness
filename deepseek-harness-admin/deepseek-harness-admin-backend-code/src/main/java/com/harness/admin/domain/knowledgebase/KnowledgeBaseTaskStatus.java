package com.harness.admin.domain.knowledgebase;

/**
 * 文件说明：知识库向量化任务状态枚举。
 *
 * <p>任务生命周期：PENDING（已创建待执行）-&gt; RUNNING（执行中）-&gt; SUCCESS（成功），
 * 失败后按有限次数重试，重试耗尽进入 FAILED。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
public enum KnowledgeBaseTaskStatus {

    /** 已创建，等待执行 */
    PENDING,

    /** 执行中 */
    RUNNING,

    /** 执行成功 */
    SUCCESS,

    /** 最终失败（重试耗尽） */
    FAILED
}
