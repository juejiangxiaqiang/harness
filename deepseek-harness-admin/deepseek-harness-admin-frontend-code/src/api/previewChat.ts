// ---
// last_updated: 2026-10-08
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：管理端预览对话接口封装（发起/延续对话、会话历史、消息历史），
// 路径对照 docs/API_SPEC.md 数字员工接口第 5 章。

import { request } from './apiClient'
import type { ChatRequestDTO, ChatResponseDTO, ConversationDTO, MessageDTO, PagedResult } from '@/types'

/**
 * 发起/延续会话并一次性获取数字员工回复。
 * @param employeeId 数字员工 ID
 * @param payload 对话请求（conversationId 为空表示新会话）
 * @returns 会话 ID 与回复
 */
export function sendChatMessage(employeeId: number, payload: ChatRequestDTO): Promise<ChatResponseDTO> {
  return request({ url: `/digital-employees/${employeeId}/chat`, method: 'POST', data: payload })
}

/**
 * 分页查询数字员工的会话历史。
 * @param employeeId 数字员工 ID
 * @param page 页码
 * @param size 每页条数
 * @returns 会话分页结果
 */
export function fetchConversations(employeeId: number, page = 1, size = 20): Promise<PagedResult<ConversationDTO>> {
  return request({ url: `/digital-employees/${employeeId}/conversations`, method: 'GET', params: { page, size } })
}

/**
 * 查询指定会话的消息列表。
 * @param employeeId 数字员工 ID
 * @param conversationId 会话 ID
 * @returns 消息列表
 */
export function fetchMessages(employeeId: number, conversationId: number): Promise<MessageDTO[]> {
  return request({ url: `/digital-employees/${employeeId}/conversations/${conversationId}/messages`, method: 'GET' })
}