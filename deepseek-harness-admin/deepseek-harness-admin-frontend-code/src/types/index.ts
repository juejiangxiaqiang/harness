// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：与后端 DTO 精确对齐的接口类型与枚举，是全站类型唯一来源；
// 字段形状对照 docs/API_SPEC.md 与后端源码（Controller/DTO 直接调研结果）。

/** 统一响应结构，对应后端 ApiResponse<T>；业务成败均由 code 区分 */
export interface ApiResponse<T> {
  code: string
  message: string
  data: T | null
  traceId: string
}

/** 分页结果，对应后端 PageResult<T>；page 从 1 开始，size 单页上限 100 */
export interface PagedResult<T> {
  page: number
  size: number
  total: number
  items: T[]
}

/** 用户角色，对应后端 UserRole 枚举值 */
export type UserRole = 'ADMIN' | 'USER'

/** 用户信息，对应后端 UserResponse */
export interface UserDTO {
  id: number
  username: string
  role: UserRole
}

/** 登录请求体：账号与密码均必填 */
export interface LoginRequestDTO {
  username: string
  password: string
}

/** 登录响应：Bearer 凭证 + 用户信息，对应后端 LoginResponse */
export interface LoginResultDTO {
  token: string
  user: UserDTO
}

/** 知识库状态，对应后端 KnowledgeBaseStatus 枚举值 */
export type KnowledgeBaseStatus = 'UPLOADING' | 'PROCESSING' | 'READY' | 'FAILED'

/** 知识库，对应后端 KnowledgeBaseResponse */
export interface KnowledgeBaseDTO {
  id: number
  name: string
  status: KnowledgeBaseStatus
  fileName: string
  chunkCount: number
  vectorStore: string
  embeddingModel: string
  /** 解析/向量化失败原因，成功后为 null */
  failureReason: string | null
  /** ISO-8601 时间字符串 */
  createdAt: string
}

/** Skill 状态，对应后端 SkillStatus 枚举值 */
export type SkillStatus = 'ACTIVE' | 'DISABLED'

/** Skill，对应后端 SkillResponse */
export interface SkillDTO {
  id: number
  name: string
  description: string
  content: string
  status: SkillStatus
  version: string
  knowledgeBaseIds: number[]
  createdAt: string
}

/** 创建 Skill 请求体：knowledgeBaseIds 至少 1 个，且必须是 READY 状态的知识库 */
export interface SkillRequestDTO {
  name: string
  description?: string
  content: string
  knowledgeBaseIds: number[]
}

/** 模型配置：后端不校验内部键，前端定义自由 JSON 对象保持兼容 */
export type ModelConfig = Record<string, unknown>

/** 数字员工状态，对应后端 DigitalEmployeeStatus 枚举值 */
export type DigitalEmployeeStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'

/** 数字员工，对应后端 DigitalEmployeeResponse */
export interface DigitalEmployeeDTO {
  id: number
  name: string
  description: string
  systemPrompt: string
  modelConfig: ModelConfig
  status: DigitalEmployeeStatus
  /** 当前生效版本，未发布为 null */
  publishedVersion: string | null
  skillIds: number[]
  createdAt: string
}

/** 创建数字员工请求体：不接收 knowledgeBaseIds，知识库由 skillIds 间接确定 */
export interface DigitalEmployeeRequestDTO {
  name: string
  description?: string
  systemPrompt: string
  skillIds: number[]
  modelConfig: ModelConfig
}

/** 发布记录状态，对应后端 ReleaseStatus 枚举值 */
export type ReleaseStatus = 'SUCCESS' | 'FAILED'

/** 发布记录，对应后端 ReleaseResponse */
export interface ReleaseDTO {
  id: number
  digitalEmployeeId: number
  version: string
  status: ReleaseStatus
  /** 发布失败原因，成功后为 null */
  failureReason: string | null
  releasedAt: string
}

/** 回滚请求体：只能回滚到本数字员工且 SUCCESS 的发布记录 */
export interface RollbackRequestDTO {
  releaseId: number
}

/** 对话消息角色，对应后端预览消息 role */
export type PreviewMessageRole = 'USER' | 'ASSISTANT'

/** 管理端预览对话请求体：message 必填，conversationId 为空表示发起新会话 */
export interface ChatRequestDTO {
  message: string
  /** 延续的会话 ID，可空 */
  conversationId?: number
}

/** 管理端预览对话响应：会话 ID + 一次性生成的完整回复 */
export interface ChatResponseDTO {
  conversationId: number
  reply: string
  createdAt: string
}

/** 会话历史，对应后端 ConversationResponse */
export interface ConversationDTO {
  id: number
  digitalEmployeeId: number
  title: string
  createdAt: string
  messageCount: number
  lastMessage: string
}

/** 会话消息，对应后端 MessageResponse */
export interface MessageDTO {
  id: number
  role: PreviewMessageRole
  content: string
  createdAt: string
}

/** 前端模型配置表单结构（温度/上下文长度），序列化进 modelConfig 的 JSON 对象 */
export interface ModelConfigForm {
  model: string
  temperature: number
  maxTokens: number
}
