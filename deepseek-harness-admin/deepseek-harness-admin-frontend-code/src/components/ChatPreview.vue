<!--
---
last_updated: 2026-10-08
status: active          # active | deprecated | draft
owner: @qiang
---
文件职责：数字员工对话验证组件。消息气泡、输入区、快捷提问、发送中指示器，
对接管理端对话接口（一次性回复）；支持会话历史加载与切换、新建对话。
对话基于数字员工当前配置与关联知识库实时作答，仅用于管理验证。
-->
<script setup lang="ts">
// @ts-ignore - 解决 vue 模块声明文件未找到的类型检查问题
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Promotion } from '@element-plus/icons-vue'
import { sendChatMessage, fetchConversations, fetchMessages } from '@/api/previewChat'
import type { ConversationDTO, PreviewMessageRole } from '@/types'

/** 对话消息视图模型 */
interface ChatCell {
  role: PreviewMessageRole
  content: string
}

const props = defineProps<{
  /** 数字员工 ID，用于对话与历史接口 */
  employeeId: number
  /** 数字员工名称，用于标题与头像 */
  employeeName?: string
}>()

const messages = ref<ChatCell[]>([])
const input = ref('')
const sending = ref(false)
const conversations = ref<ConversationDTO[]>([])
const activeConversationId = ref<number | null>(null)
const listRef = ref<HTMLDivElement | null>(null)

/** 快捷提问：面向客服场景的常用问题 */
const quickQuestions = ['帮我介绍一下岗位职责', '公司的请假制度是什么？', '如何申请年假？']

/** 加载该数字员工的会话历史列表 */
async function loadConversations() {
  try {
    const result = await fetchConversations(props.employeeId, 1, 20)
    conversations.value = result.items
  } catch {
    conversations.value = []
  }
}

/** 选中历史会话并加载其消息 */
async function selectConversation(id: number) {
  activeConversationId.value = id
  messages.value = []
  try {
    const items = await fetchMessages(props.employeeId, id)
    messages.value = items.map((m: { role: PreviewMessageRole; content: string }) => ({
      role: m.role,
      content: m.content
    }))
  } catch {
    ElMessage.error('会话历史加载失败')
  }
  nextTick(() => scrollToBottom())
}

/** 历史会话下拉变更处理 */
function onSelectConversation(event: Event) {
  const value = (event.target as HTMLSelectElement).value
  if (value === '') {
    newConversation()
  } else {
    selectConversation(Number(value))
  }
}

/** 开始新会话：清空消息与当前会话 ID */
function newConversation() {
  activeConversationId.value = null
  messages.value = []
  scrollToBottom()
}

/** 滚动消息区到底部，保证新消息可见 */
function scrollToBottom() {
  if (listRef.value) {
    listRef.value.scrollTop = listRef.value.scrollHeight
  }
}

/** 发送消息：调用对话接口取回复并落库会话 */
async function send(text: string) {
  const content = text.trim()
  if (!content || sending.value) return
  // 先乐观追加用户消息，随后请求真实回复
  messages.value.push({ role: 'USER', content })
  input.value = ''
  sending.value = true
  nextTick(() => scrollToBottom())
  try {
    const response = await sendChatMessage(props.employeeId, {
      message: content,
      conversationId: activeConversationId.value ?? undefined
    })
    messages.value.push({ role: 'ASSISTANT', content: response.reply })
    activeConversationId.value = response.conversationId
    await loadConversations()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '对话失败，请重试')
  } finally {
    sending.value = false
    nextTick(() => scrollToBottom())
  }
}

onMounted(() => {
  loadConversations()
})
</script>

<template>
  <div class="chat card">
    <header class="chat-head">
      <span class="dot-pulse" aria-label="在线" />
      <span class="chat-title">{{ employeeName || '数字员工' }}</span>
      <span class="chat-badge">对话验证</span>
      <button type="button" class="new-chat" :disabled="sending" @click="newConversation">新对话</button>
    </header>

    <div v-if="conversations.length" class="history">
      <span class="history-label">历史会话：</span>
      <select class="history-select" :value="activeConversationId ?? ''" @change="onSelectConversation">
        <option value="">（新对话）</option>
        <option v-for="c in conversations" :key="c.id" :value="c.id">
          {{ (c.title || '会话') + ' · ' + c.messageCount + ' 条' }}
        </option>
      </select>
    </div>

    <div ref="listRef" class="chat-body">
      <div v-if="messages.length === 0" class="chat-empty">
        你好，我是「{{ employeeName || '数字员工' }}」。输入问题或点击下方快捷提问开始，回答基于当前配置与关联知识库实时生成。
      </div>
      <div v-for="(msg, idx) in messages" :key="idx" class="msg-row" :class="msg.role === 'USER' ? 'user' : 'assistant'">
        <div v-if="msg.role === 'ASSISTANT'" class="msg-avatar">{{ employeeName?.charAt(0) || '员' }}</div>
        <div class="msg-bubble">{{ msg.content }}</div>
      </div>
      <div v-if="sending" class="msg-row assistant">
        <div class="msg-avatar">{{ employeeName?.charAt(0) || '员' }}</div>
        <div class="msg-bubble typing-indicator"><span /><span /><span /></div>
      </div>
    </div>

    <div class="quick-questions">
      <button v-for="q in quickQuestions" :key="q" type="button" class="quick-pill" :disabled="sending" @click="send(q)">
        {{ q }}
      </button>
    </div>

    <div class="omni-box">
      <textarea
        v-model="input"
        rows="1"
        class="omni-input"
        placeholder="输入问题，回车发送"
        :disabled="sending"
        @keydown.enter.prevent="send(input)"
      />
      <button type="button" class="send-btn" aria-label="发送" :disabled="!input.trim() || sending" @click="send(input)">
        <el-icon :size="16"><Promotion /></el-icon>
      </button>
    </div>
    <p class="chat-note">基于数字员工当前配置与关联知识库实时作答，仅用于管理验证</p>
  </div>
</template>

<style scoped>
.chat { display: flex; flex-direction: column; height: 100%; min-height: 520px; overflow: hidden; }
.chat-head { display: flex; align-items: center; gap: var(--space-1); padding: var(--space-3); border-bottom: 1px solid var(--color-border-default); }
.chat-title { font-size: var(--font-size-card); font-weight: var(--font-weight-card); flex: 1; }
.chat-badge { font-size: var(--font-size-small); color: var(--color-text-secondary); background: #f5f5f5; padding: 2px 8px; border-radius: var(--radius-pill); }
.new-chat { border: 1px solid var(--color-primary); background: transparent; color: var(--color-primary); font-size: var(--font-size-small); padding: 2px 10px; border-radius: var(--radius-pill); cursor: pointer; }
.new-chat:disabled { opacity: 0.5; cursor: not-allowed; }
.history { display: flex; align-items: center; gap: 6px; padding: 6px var(--space-3); background: var(--color-bg-page); border-bottom: 1px solid var(--color-border-default); }
.history-label { font-size: var(--font-size-small); color: var(--color-text-secondary); flex-shrink: 0; }
.history-select { flex: 1; font-size: var(--font-size-small); border: 1px solid var(--color-border-default); border-radius: var(--radius-sm); padding: 3px 6px; background: var(--color-bg-surface); }
.chat-body { flex: 1; overflow-y: auto; padding: var(--space-3); background: #f7f8fa; display: flex; flex-direction: column; gap: var(--space-2); }
.chat-empty { text-align: center; color: var(--color-text-secondary); font-size: var(--font-size-body); line-height: 1.6; padding: var(--space-6) var(--space-4); }
.msg-row { display: flex; gap: var(--space-1); align-items: flex-start; max-width: 82%; }
.msg-row.user { align-self: flex-end; flex-direction: row-reverse; }
.msg-avatar { width: 28px; height: 28px; border-radius: 50%; flex-shrink: 0; background: linear-gradient(135deg, var(--color-accent-purple), var(--color-primary)); color: #fff; font-size: var(--font-size-small); display: flex; align-items: center; justify-content: center; }
.msg-bubble { padding: 8px 12px; border-radius: 8px; font-size: var(--font-size-body); line-height: 1.6; white-space: pre-wrap; word-break: break-word; }
.msg-row.user .msg-bubble { background: var(--color-primary); color: #fff; border-bottom-right-radius: 2px; }
.msg-row.assistant .msg-bubble { background: var(--color-bg-surface); border: 1px solid var(--color-border-default); color: var(--color-text-main); border-bottom-left-radius: 2px; }
.typing-indicator { display: flex; gap: 4px; padding: 10px 12px; }
.typing-indicator span { width: 6px; height: 6px; border-radius: 50%; background: var(--color-text-placeholder); animation: bounce 1s ease-in-out infinite; }
.typing-indicator span:nth-child(2) { animation-delay: 0.15s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.3s; }
@keyframes bounce { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-4px); } }
.quick-questions { display: flex; flex-wrap: wrap; gap: var(--space-1); padding: var(--space-2) var(--space-3); }
.quick-pill { border: 1px solid var(--color-border-default); background: var(--color-bg-surface); color: var(--color-text-secondary); border-radius: var(--radius-pill); padding: 4px 12px; font-size: var(--font-size-small); cursor: pointer; transition: var(--transition-base); }
.quick-pill:hover { border-color: var(--color-primary); color: var(--color-primary); }
.quick-pill:disabled { opacity: 0.5; cursor: not-allowed; }
.omni-box { display: flex; align-items: center; gap: 6px; margin: var(--space-2) var(--space-3) var(--space-3); padding: 6px 8px; background: var(--color-bg-surface); border: 1px solid var(--color-border-default); border-radius: var(--radius-pill); box-shadow: var(--shadow-sm); transition: var(--transition-base); }
.omni-box:focus-within { border-color: var(--color-primary); box-shadow: 0 0 0 3px var(--color-focus-ring); }
.omni-input { flex: 1; resize: none; border: none; outline: none; background: transparent; padding: 6px 8px; font-family: inherit; font-size: var(--font-size-body); line-height: 1.5; max-height: 60px; color: var(--color-text-main); }
.omni-input::placeholder { color: var(--color-text-placeholder); }
.omni-input:disabled { cursor: not-allowed; }
.send-btn { width: 36px; height: 36px; flex-shrink: 0; border-radius: 50%; border: none; background: var(--color-primary); color: #fff; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: var(--transition-base); }
.send-btn:hover { background: var(--color-primary-hover); }
.send-btn:disabled { background: var(--color-border-default); cursor: not-allowed; }
.chat-note { margin: 0; padding: 0 var(--space-3) var(--space-2); font-size: var(--font-size-small); color: var(--color-text-placeholder); }
@media (prefers-reduced-motion: reduce) { .typing-indicator span { animation: none; } }
</style>