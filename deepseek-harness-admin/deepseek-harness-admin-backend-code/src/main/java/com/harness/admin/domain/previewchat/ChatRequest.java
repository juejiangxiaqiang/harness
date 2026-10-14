package com.harness.admin.domain.previewchat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 文件说明：管理端预览对话请求模型。
 *
 * <p>对话消息必填且限定长度；{@code conversationId} 可空，为空表示发起新会话，
 * 非空表示在该会话中继续追加消息。</p>
 *
 * @param message        用户消息内容
 * @param conversationId 延续的会话 ID（可空）
 * @author harness-admin
 * @since 2026-10-08
 */
public record ChatRequest(
        @NotBlank(message = "对话消息不能为空")
        @Size(max = 4000, message = "消息长度不能超过 4000 字符")
        String message,

        Long conversationId) {
}