package com.harness.admin.domain.previewchat;

import java.time.LocalDateTime;

/**
 * 文件说明：管理端预览对话响应模型。
 *
 * <p>携带所属会话 ID 与本次生成回复；前端以该会话 ID 延续后续对话并保存历史。</p>
 *
 * @param conversationId 会话 ID
 * @param reply          数字员工一次生成的完整回复
 * @param createdAt      回复时间
 * @author harness-admin
 * @since 2026-10-08
 */
public record ChatResponse(Long conversationId, String reply, LocalDateTime createdAt) {
}