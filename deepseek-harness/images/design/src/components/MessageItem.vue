<!-- 文件路径: design/src/components/MessageItem.vue -->
<!-- 作用: 消息气泡 —— 用户右侧蓝底白字，助手左侧白底带圆形头像；支持流式打字与思考动画 -->
<template>
  <div class="message-item" :class="[message.role]">
    <!-- 助手消息：左侧圆形头像（单字） -->
    <EmployeeAvatar
      v-if="message.role === 'assistant'"
      :char="avatarChar"
      :color="avatarColor"
      :size="36"
    />

    <div class="message-body">
      <!-- 气泡内容 -->
      <div class="bubble">
        <!-- 正在流式输出且内容为空时展示思考动画 -->
        <span v-if="message.role === 'assistant' && !message.content && streaming" class="thinking">
          <i />
          <i />
          <i />
        </span>
        <!-- 消息文本：保留换行 -->
        <span v-else class="bubble-text">{{ message.content }}</span>
      </div>
      <!-- 时间戳（次要信息） -->
      <span class="message-time">{{ formatTime(message.time) }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
// 单条对话消息
import EmployeeAvatar from '@/components/EmployeeAvatar.vue'
import type { ChatMessage } from '@/types'

// 消息数据 + 助手侧头像信息
const props = withDefaults(
  defineProps<{
    message: ChatMessage
    streaming?: boolean // 是否处于流式回复（用于思考动画）
    avatarChar?: string // 助手头像单字
    avatarColor?: string // 助手头像底色
  }>(),
  { streaming: false, avatarChar: '助', avatarColor: '#2563eb' }
)

// 格式化消息时间（HH:mm）
function formatTime(ts: number): string {
  const d = new Date(ts)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// 校验 props 仅用于类型推导引用
void props
</script>

<style scoped>
/* 消息行：纵向排列，行内留白 */
.message-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 8px 0;
}
/* 用户消息：右侧对齐（头像留空保持对称） */
.message-item.user {
  flex-direction: row-reverse;
}

/* 消息内容列 */
.message-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 72%;
}

/* 气泡：白底 12px 圆角（助手），蓝底白字（用户） */
.bubble {
  padding: 12px 16px;
  border-radius: var(--radius-lg);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  word-break: break-word;
  white-space: pre-wrap;
  box-shadow: var(--shadow-card);
}
/* 助手气泡：白底，左上角大圆角 */
.message-item.assistant .bubble {
  background: var(--color-surface);
  border: 1px solid var(--color-border-light);
  border-top-left-radius: var(--radius-sm);
}
/* 用户气泡：品牌蓝，右上角小圆角 */
.message-item.user .bubble {
  background: var(--color-primary);
  color: var(--color-text-invert);
  border-top-right-radius: var(--radius-sm);
}

/* 时间戳 */
.message-time {
  font-size: var(--font-size-caption);
  color: var(--color-text-muted);
  padding: 0 4px;
}

/* 思考动画：三个圆点依次脉动 */
.thinking {
  display: inline-flex;
  gap: 5px;
  align-items: center;
  min-height: 20px;
}
.thinking i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-primary);
  animation: thinking-pulse 1.2s infinite;
}
.thinking i:nth-child(2) {
  animation-delay: 0.2s;
}
.thinking i:nth-child(3) {
  animation-delay: 0.4s;
}
@keyframes thinking-pulse {
  0%,
  60%,
  100% {
    opacity: 0.25;
    transform: scale(0.85);
  }
  30% {
    opacity: 1;
    transform: scale(1);
  }
}
</style>
