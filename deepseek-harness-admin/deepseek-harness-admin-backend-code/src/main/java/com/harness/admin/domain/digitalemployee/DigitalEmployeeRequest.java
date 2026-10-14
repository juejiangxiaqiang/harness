package com.harness.admin.domain.digitalemployee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * 文件说明：数字员工创建请求模型。
 *
 * <p>只接收名称、描述、系统提示词、模型配置与关联 Skill ID；
 * 不接收知识库 ID（数字员工的知识库由所选 Skills 间接提供），
 * 多余字段（如 knowledgeBaseIds）由反序列化配置忽略。</p>
 *
 * @param name          数字员工名称
 * @param description   数字员工描述
 * @param systemPrompt  系统提示词
 * @param skillIds      关联 Skill ID 列表（至少一个）
 * @param modelConfig   模型配置（JSON 对象）
 * @author harness-admin
 * @since 2026-10-06
 */
public record DigitalEmployeeRequest(
        @NotBlank(message = "数字员工名称不能为空")
        @Size(max = 128, message = "数字员工名称长度不能超过 128")
        String name,

        @Size(max = 500, message = "数字员工描述长度不能超过 500")
        String description,

        @NotBlank(message = "系统提示词不能为空")
        @Size(max = 20000, message = "系统提示词长度不能超过 20000")
        String systemPrompt,

        @NotEmpty(message = "至少关联一个 Skill")
        List<Long> skillIds,

        @NotEmpty(message = "模型配置不能为空")
        Map<String, Object> modelConfig) {
}
