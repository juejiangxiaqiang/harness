package com.harness.admin.domain.skill;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 文件说明：Skill 创建请求模型。
 *
 * <p>包含基本信息与一个或多个知识库 ID。知识库为全局共享资源，
 * 关联时服务端校验其存在且处于 READY 状态，不信任请求体中的操作者字段。</p>
 *
 * @param name             Skill 名称
 * @param description      Skill 描述
 * @param content          Skill 配置或实现内容
 * @param knowledgeBaseIds 关联知识库 ID 列表（至少一个）
 * @author harness-admin
 * @since 2026-10-06
 */
public record SkillRequest(
        @NotBlank(message = "Skill 名称不能为空")
        @Size(max = 128, message = "Skill 名称长度不能超过 128")
        String name,

        @Size(max = 500, message = "Skill 描述长度不能超过 500")
        String description,

        @NotBlank(message = "Skill 内容不能为空")
        @Size(max = 10000, message = "Skill 内容长度不能超过 10000")
        String content,

        @NotEmpty(message = "至少关联一个知识库")
        List<Long> knowledgeBaseIds) {
}
