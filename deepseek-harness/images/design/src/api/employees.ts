// 文件路径: design/src/api/employees.ts
// 作用: 数字员工相关接口（docs/2.0/后端接口文档.md §8/§9）+ 接口字段到卡片视图模型的映射
// 路径: GET /digital-employees、GET /digital-employees/{id}、
//       POST /digital-employees/{id}/chat、GET .../conversations、GET .../conversations/{conversationId}/messages
import request from './request'
import { loadFavorites } from '@/utils/storage'
import type {
  ChatResult,
  ConversationResponse,
  DigitalEmployeeResponse,
  Employee,
  MessageResponse,
  PageResult
} from '@/types'

// 获取数字员工列表（分页）
export function fetchEmployees(page = 1, size = 12): Promise<PageResult<DigitalEmployeeResponse>> {
  return request.get('/digital-employees', { params: { page, size } }) as Promise<
    PageResult<DigitalEmployeeResponse>
  >
}

// 获取数字员工详情（进入对话页时校验存在/取标题）
export function fetchEmployee(id: number | string): Promise<DigitalEmployeeResponse> {
  return request.get(`/digital-employees/${id}`) as Promise<DigitalEmployeeResponse>
}

// 分页查询会话历史
export function fetchConversations(
  id: number | string,
  page = 1,
  size = 20
): Promise<PageResult<ConversationResponse>> {
  return request.get(`/digital-employees/${id}/conversations`, { params: { page, size } }) as Promise<
    PageResult<ConversationResponse>
  >
}

// 查询指定会话的全部消息（正序）
export function fetchMessages(
  id: number | string,
  conversationId: number | string
): Promise<MessageResponse[]> {
  return request.get(`/digital-employees/${id}/conversations/${conversationId}/messages`) as Promise<
    MessageResponse[]
  >
}

// 发起/延续会话并对话（一次性返回完整回复，非流式）
export function sendChat(
  id: number | string,
  message: string,
  conversationId?: number
): Promise<ChatResult> {
  return request.post(`/digital-employees/${id}/chat`, { message, conversationId }) as Promise<
    ChatResult
  >
}

// 图标底色业务色板（设计稿图标色相编码）
const PALETTE = ['#2563eb', '#7c3aed', '#0891b2', '#4f46e5', '#0d9488', '#e11d48', '#ea580c']

// 接口字段 → 卡片视图模型（docs/2.0/前端改造计划文档.md §3.3 字段映射表）
// color/iconChar/tags/inUse 均无接口字段，按派生规则生成
export function toEmployeeCard(raw: DigitalEmployeeResponse): Employee {
  const id = String(raw.id)
  return {
    id,
    name: raw.name,
    iconChar: raw.name.trim().charAt(0) || '员',
    color: PALETTE[Math.abs(raw.id) % PALETTE.length],
    desc: raw.description ?? '',
    tags: [], // 接口无标签字段，无数据时隐藏标签组
    favorite: loadFavorites().includes(id),
    inUse: raw.status === 'PUBLISHED',
    status: raw.status
  }
}
