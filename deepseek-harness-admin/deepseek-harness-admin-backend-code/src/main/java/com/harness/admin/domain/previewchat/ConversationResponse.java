package com.harness.admin.domain.previewchat;

import java.time.LocalDateTime;

/**
 * 文件说明：会话历史响应模型。
 *
 * <p>用于管理端预览页展示数字员工的对话历史列表，含消息数与最近一条消息摘要。</p>
 *
 * @param id              会话 ID
 * @param digitalEmployeeId 所属数字员工 ID
 * @param title           会话标题
 * @param createdAt       会话创建时间
 * @param messageCount    会话内消息数
 * @param lastMessage     最近一条消息摘要
 * @author harness-admin
 * @since 2026-10-08
 */
public record ConversationResponse(
        Long id,
        Long digitalEmployeeId,
        String title,
        LocalDateTime createdAt,
        Long messageCount,
        String lastMessage) {
}