package com.harness.admin.domain.skill;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件说明：Skill 响应模型。
 *
 * <p>包含 Skill 基本信息与关联知识库 ID 列表。</p>
 *
 * @param id              Skill ID
 * @param name            Skill 名称
 * @param description     Skill 描述
 * @param content         Skill 内容
 * @param status          Skill 状态
 * @param version         Skill 版本
 * @param knowledgeBaseIds 关联知识库 ID 列表
 * @param createdAt       创建时间
 * @author harness-admin
 * @since 2026-10-06
 */
public record SkillResponse(
        Long id,
        String name,
        String description,
        String content,
        SkillStatus status,
        String version,
        List<Long> knowledgeBaseIds,
        LocalDateTime createdAt) {
}
