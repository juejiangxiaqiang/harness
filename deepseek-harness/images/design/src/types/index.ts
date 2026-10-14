// 文件路径: design/src/types/index.ts
// 作用: 全局类型定义 —— 数字员工、对话消息、会话、用户、后端接口契约

// 数字员工实体（卡片视图模型，由后端 DigitalEmployeeResponse 映射派生）
export interface Employee {
  id: string
  name: string // 员工名称
  iconChar: string // 圆形图标内的单字（取名称首字）
  color: string // 图标底色（按 id 哈希派生业务色板）
  category?: string // 所属分类（后端无此字段，仅保留可选位）
  desc: string // 描述文案
  tags: string[] // 底部功能标签（后端无此字段，无数据时隐藏）
  favorite: boolean // 是否收藏（前端本地态）
  inUse: boolean // 是否已使用（status == PUBLISHED 派生）
  status: string // 后端状态：DRAFT/PUBLISHED/ARCHIVED
}

// ===== 后端统一响应契约（deepseek-harness-admin，详见 docs/2.0/后端接口文档.md） =====

// 统一响应外壳：业务成败以 code 判断，HTTP 恒为 200
export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  traceId: string
}

// 用户信息（登录/注册返回）
export interface UserResponse {
  id: number
  username: string
  role: 'ADMIN' | 'USER'
}

// 登录成功载荷
export interface LoginResult {
  token: string
  user: UserResponse
}

// 分页结果
export interface PageResult<T> {
  page: number
  size: number
  total: number
  items: T[]
}

// 数字员工（后端详情/列表元素）
export interface DigitalEmployeeResponse {
  id: number
  name: string
  description: string
  systemPrompt: string
  modelConfig: Record<string, unknown>
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
  publishedVersion: string | null
  skillIds: number[]
  createdAt: string
}

// 会话（后端会话历史元素）
export interface ConversationResponse {
  id: number
  digitalEmployeeId: number
  title: string
  createdAt: string
  messageCount: number
  lastMessage: string
}

// 会话消息（后端消息元素，role 为 USER/ASSISTANT）
export interface MessageResponse {
  id: number
  role: 'USER' | 'ASSISTANT'
  content: string
  createdAt: string
}

// 对话响应（一次性返回完整回复，非流式）
export interface ChatResult {
  conversationId: number
  reply: string
  createdAt: string
}

// 消息角色
export type MessageRole = 'user' | 'assistant'

// 单条对话消息
export interface ChatMessage {
  id: string
  role: MessageRole
  content: string
  time: number // 时间戳
}

// 会话（历史记录单元）
export interface ChatSession {
  id: string
  title: string // 会话标题（默认取首条用户消息）
  messages: ChatMessage[]
  createdAt: number
  updatedAt: number
}

// 登录用户信息（后端 UserResponse 结构）
export interface AuthUser {
  id: number
  username: string
  role: 'ADMIN' | 'USER'
}

// 会话设置项
export interface ChatSettings {
  model: string // 模型名称
  temperature: number // 温度
  systemPrompt: string // 系统提示词
  enterToSend: boolean // 回车发送
}
