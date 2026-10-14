// 文件路径: design/src/composables/useEmployeeChat.ts
// 作用: 数字员工对话核心逻辑（组合式函数）—— 对接会话历史/消息/对话三接口
// 依据: docs/2.0/前端改造计划文档.md §3.4
// 说明: chat 接口一次性返回完整回复（非流式），前端保留逐字动画作为视觉效果
import { ref } from 'vue'
import type {
  ChatMessage,
  ConversationResponse,
  DigitalEmployeeResponse,
  MessageResponse
} from '@/types'
import { fetchConversations, fetchMessages, sendChat, fetchEmployee } from '@/api/employees'

// 打字流式回复的间隔与起始延迟
const STREAM_INTERVAL = 30 // 每 30ms 追加一个字符
const REPLY_DELAY = 600 // 回复前等待时长

// 生成唯一 id
function uid(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

// 后端消息 → 前端气泡模型（角色映射：USER → user，ASSISTANT → assistant）
function toChatMessage(m: MessageResponse): ChatMessage {
  return {
    id: String(m.id),
    role: m.role === 'USER' ? 'user' : 'assistant',
    content: m.content,
    time: Date.parse(m.createdAt)
  }
}

// 对话逻辑入口：employeeId 为当前数字员工 id
export function useEmployeeChat(employeeId: string) {
  // 数字员工详情（进入页面时校验存在/取标题）
  const detail = ref<DigitalEmployeeResponse | null>(null)
  // 会话历史列表（分页追加）
  const conversations = ref<ConversationResponse[]>([])
  const convPage = ref(1)
  const convTotal = ref(0)
  const convLoading = ref(false)
  const convFinished = ref(false)
  // 当前选中会话 id（null 表示尚未开始新会话）
  const activeConversationId = ref<number | null>(null)
  // 当前会话消息
  const messages = ref<ChatMessage[]>([])
  // 消息加载状态
  const loadingMessages = ref(false)
  // 是否正在逐字输出回复
  const isStreaming = ref(false)
  // 是否正在请求对话接口
  const sending = ref(false)

  // 逐字输出定时器句柄
  let streamTimer: ReturnType<typeof setInterval> | null = null

  // 停止逐字输出并保留已输出内容
  function stopStreaming(): void {
    if (streamTimer) {
      clearInterval(streamTimer)
      streamTimer = null
    }
    isStreaming.value = false
  }

  // 加载数字员工详情（不存在时抛 RESOURCE_NOT_FOUND）
  async function loadDetail(): Promise<DigitalEmployeeResponse> {
    const data = await fetchEmployee(employeeId)
    detail.value = data
    return data
  }

  // 加载会话历史下一页（滚动到底触发）
  async function loadConversations(): Promise<void> {
    if (convLoading.value || convFinished.value) return
    convLoading.value = true
    try {
      const data = await fetchConversations(employeeId, convPage.value)
      conversations.value.push(...data.items)
      convTotal.value = data.total
      convPage.value += 1
      if (conversations.value.length >= convTotal.value) {
        convFinished.value = true
      }
    } finally {
      convLoading.value = false
    }
  }

  // 重置会话分页并重新加载首屏（新建会话后刷新列表）
  async function reloadConversations(): Promise<void> {
    convPage.value = 1
    convFinished.value = false
    conversations.value = []
    await loadConversations()
  }

  // 选中会话并拉取全部消息（正序渲染）
  async function selectConversation(id: number): Promise<void> {
    stopStreaming()
    activeConversationId.value = id
    loadingMessages.value = true
    messages.value = []
    try {
      const list = await fetchMessages(employeeId, id)
      messages.value = list.map(toChatMessage)
    } finally {
      loadingMessages.value = false
    }
  }

  // 新建会话：清空消息并等待下一条消息触发新会话
  function startNewConversation(): void {
    stopStreaming()
    activeConversationId.value = null
    messages.value = []
  }

  // 发送消息：先本地回显用户消息，再调 chat 接口，成功后用返回 reply 追加助手消息
  // conversationId 为空 → 服务端发起新会话并回填会话列表
  async function sendText(text: string): Promise<void> {
    const content = text.trim()
    if (!content || sending.value) return

    sending.value = true
    // 本地回显用户消息
    messages.value.push({ id: uid(), role: 'user', content, time: Date.now() })

    try {
      const result = await sendChat(employeeId, content, activeConversationId.value ?? undefined)
      // 新会话：服务端返回新 conversationId 并回填会话列表
      if (activeConversationId.value === null) {
        activeConversationId.value = result.conversationId
        await reloadConversations()
      }
      // 追加空助手消息，用返回 reply 逐字展示（内容一次性取自接口）
      const assistant: ChatMessage = { id: uid(), role: 'assistant', content: '', time: Date.now() }
      messages.value.push(assistant)
      isStreaming.value = true
      await new Promise<void>((resolve) => {
        setTimeout(() => {
          let index = 0
          streamTimer = setInterval(() => {
            index += 1
            assistant.content = result.reply.slice(0, index)
            if (index >= result.reply.length) {
              stopStreaming()
              assistant.content = result.reply
              resolve()
            }
          }, STREAM_INTERVAL)
        }, REPLY_DELAY)
      })
    } finally {
      sending.value = false
    }
  }

  return {
    detail,
    conversations,
    activeConversationId,
    messages,
    convLoading,
    loadingMessages,
    isStreaming,
    sending,
    hasMoreConversations: () => !convFinished.value,
    loadDetail,
    loadConversations,
    selectConversation,
    startNewConversation,
    sendText,
    stopStreaming
  }
}

// 组合式函数的返回类型（供子组件 props 声明使用）
export type UseEmployeeChat = ReturnType<typeof useEmployeeChat>
