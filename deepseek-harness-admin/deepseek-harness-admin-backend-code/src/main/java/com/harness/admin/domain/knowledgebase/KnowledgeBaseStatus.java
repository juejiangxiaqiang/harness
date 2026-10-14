package com.harness.admin.domain.knowledgebase;

/**
 * 文件说明：知识库处理状态枚举。
 *
 * <p>状态流转：UPLOADING -&gt; PROCESSING -&gt; READY，任一环节失败进入 FAILED。
 * 仅 {@code READY} 状态的知识库可被 Skill 关联。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
public enum KnowledgeBaseStatus {

    /** 已创建上传记录，文件尚未开始向量化 */
    UPLOADING,

    /** 正在解析、切分、生成向量或写入向量数据库 */
    PROCESSING,

    /** 向量化处理完成，可被 Skill 选择 */
    READY,

    /** 处理失败，需记录失败原因并支持重新处理 */
    FAILED
}
