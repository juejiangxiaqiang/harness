package com.harness.admin.domain.previewchat;

import java.time.LocalDateTime;

/**
 * 文件说明：会话消息响应模型。
 *
 * @param id        消息 ID
 * @param role      消息角色：USER 或 ASSISTANT
 * @param content   消息内容
 * @param createdAt 消息时间
 * @author harness-admin
 * @since 2026-10-08
 */
public record MessageResponse(Long id, String role, String content, LocalDateTime createdAt) {
}