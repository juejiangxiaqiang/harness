package com.harness.admin.domain.digitalemployee;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 文件说明：数字员工响应模型。
 *
 * <p>包含数字员工配置、状态、发布版本与关联 Skill ID 列表。</p>
 *
 * @param id              数字员工 ID
 * @param name            名称
 * @param description     描述
 * @param systemPrompt    系统提示词
 * @param modelConfig     模型配置
 * @param status          状态
 * @param publishedVersion 当前发布版本
 * @param skillIds        关联 Skill ID 列表
 * @param createdAt       创建时间
 * @author harness-admin
 * @since 2026-10-06
 */
public record DigitalEmployeeResponse(
        Long id,
        String name,
        String description,
        String systemPrompt,
        Map<String, Object> modelConfig,
        DigitalEmployeeStatus status,
        String publishedVersion,
        List<Long> skillIds,
        LocalDateTime createdAt) {

    /**
     * 转换为普通用户可见视图：隐藏系统提示词、模型配置、发布版本与 Skill 关联等内部配置，
     * 仅保留浏览/对话所需的展示字段。
     *
     * @return 裁剪后的响应
     */
    public DigitalEmployeeResponse toUserView() {
        return new DigitalEmployeeResponse(id, name, description, null, null, status, null, List.of(), createdAt);
    }
}
