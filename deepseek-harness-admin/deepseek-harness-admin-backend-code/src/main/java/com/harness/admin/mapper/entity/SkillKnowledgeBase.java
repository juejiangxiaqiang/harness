package com.harness.admin.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 文件说明：Skill 与知识库关联持久化实体。
 *
 * <p>对应 {@code skill_knowledge_base} 表，联合唯一约束防止重复关联。
 * Skill 只能关联 READY 状态知识库（由 Service 层校验）。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Data
@TableName("`skill_knowledge_base`")
public class SkillKnowledgeBase {

    /** 关联主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Skill ID */
    private Long skillId;

    /** 知识库 ID */
    private Long knowledgeBaseId;
}
