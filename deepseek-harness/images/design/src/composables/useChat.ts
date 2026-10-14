// 文件路径: design/src/composables/useChat.ts
// 作用: 对话核心逻辑（组合式函数）—— 会话管理、消息收发、打字流式回复、设置持久化
// 同时服务于 deepseek harness 对话页与数字员工对话页
import { computed, onUnmounted, ref } from 'vue'
import type { ChatMessage, ChatSession, ChatSettings } from '@/types'
import { mockReply, type Persona } from '@/mock/chat'
import { loadSessions, saveSessions, loadSettings, saveSettings } from '@/utils/storage'

// 打字流式回复的间隔与起始延迟
const STREAM_INTERVAL = 30 // 每 30ms 追加一个字符
const REPLY_DELAY = 600 // 回复前等待时长

// 生成唯一 id
function uid(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

// 对话逻辑入口：scope 用于隔离不同场景的会话存储（如 harness / employee-e1）
export function useChat(scope: string, persona: Persona) {
  // 会话列表（按更新时间倒序）
  const sessions = ref<ChatSession[]>(loadSessions(scope))
  // 当前激活会话 id
  const activeId = ref<string>(sessions.value[0]?.id ?? '')
  // 是否正在流式回复
  const isStreaming = ref(false)
  // 对话设置（模型/温度/系统提示/回车发送）
  const settings = ref<ChatSettings>(loadSettings())

  // 流式回复定时器句柄
  let streamTimer: ReturnType<typeof setInterval> | null = null
  // 回复启动延迟句柄
  let replyTimer: ReturnType<typeof setTimeout> | null = null

  // 当前会话（不存在时自动兜底为最新会话）
  const activeSession = computed<ChatSession>(() => {
    const found = sessions.value.find((s) => s.id === activeId.value)
    return found ?? sessions.value[0]
  })

  // 当前会话的消息列表
  const messages = computed<ChatMessage[]>(() => activeSession.value?.messages ?? [])

  // 持久化会话列表（按更新时间倒序）
  function persist(): void {
    sessions.value = [...sessions.value].sort((a, b) => b.updatedAt - a.updatedAt)
    saveSessions(scope, sessions.value)
  }

  // 确保存在可用会话（无会话时自动新建）
  function ensureSession(): void {
    if (sessions.value.length === 0) {
      createSession()
    }
  }

  // 创建新会话并切换过去
  function createSession(): ChatSession {
    const session: ChatSession = {
      id: uid(),
      title: '新会话',
      messages: [],
      createdAt: Date.now(),
      updatedAt: Date.now()
    }
    sessions.value.unshift(session)
    activeId.value = session.id
    persist()
    return session
  }

  // 切换会话
  function selectSession(id: string): void {
    if (isStreaming.value) stopStreaming()
    activeId.value = id
  }

  // 删除会话
  function removeSession(id: string): void {
    if (isStreaming.value) stopStreaming()
    sessions.value = sessions.value.filter((s) => s.id !== id)
    // 删除的是当前会话时，切换到最新会话
    if (activeId.value === id) {
      activeId.value = sessions.value[0]?.id ?? ''
    }
    persist()
  }

  // 重命名会话
  function renameSession(id: string, title: string): void {
    const session = sessions.value.find((s) => s.id === id)
    if (session) {
      session.title = title.trim() || '新会话'
      persist()
    }
  }

  // 停止流式回复并保留已输出内容
  function stopStreaming(): void {
    if (streamTimer) {
      clearInterval(streamTimer)
      streamTimer = null
    }
    if (replyTimer) {
      clearTimeout(replyTimer)
      replyTimer = null
    }
    isStreaming.value = false
  }

  // 发送一条消息：追加用户消息，再流式输出助手回复
  function sendText(text: string): void {
    const content = text.trim()
    if (!content || isStreaming.value) return

    ensureSession()
    const session = activeSession.value
    const now = Date.now()
    // 追加用户消息
    session.messages.push({ id: uid(), role: 'user', content, time: now })
    // 首条消息时以用户内容作为会话标题
    if (session.title === '新会话' || session.messages.filter((m) => m.role === 'user').length === 1) {
      session.title = content.slice(0, 20)
    }
    session.updatedAt = now

    // 预生成完整回复内容（本地演示），随后逐字流式展示
    const fullReply = mockReply(content, persona)
    // 追加空的助手消息，流式填充
    const assistant: ChatMessage = { id: uid(), role: 'assistant', content: '', time: Date.now() }
    session.messages.push(assistant)
    persist()

    // 延迟后开始逐字输出
    isStreaming.value = true
    replyTimer = setTimeout(() => {
      let index = 0
      streamTimer = setInterval(() => {
        index += 1
        assistant.content = fullReply.slice(0, index)
        // 输出完整后收尾
        if (index >= fullReply.length) {
          stopStreaming()
          assistant.content = fullReply
          session.updatedAt = Date.now()
          persist()
        }
      }, STREAM_INTERVAL)
    }, REPLY_DELAY)
  }

  // 保存对话设置
  function updateSettings(patch: Partial<ChatSettings>): void {
    settings.value = { ...settings.value, ...patch }
    saveSettings(settings.value)
  }

  // 组件卸载时清理定时器
  onUnmounted(stopStreaming)

  return {
    sessions,
    activeId,
    messages,
    isStreaming,
    settings,
    ensureSession,
    createSession,
    selectSession,
    removeSession,
    renameSession,
    sendText,
    stopStreaming,
    updateSettings
  }
}

// 组合式函数的返回类型（供子组件 props 声明使用）
export type UseChat = ReturnType<typeof useChat>

