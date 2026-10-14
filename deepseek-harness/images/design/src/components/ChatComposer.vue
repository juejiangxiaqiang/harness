<!-- 文件路径: design/src/components/ChatComposer.vue -->
<!-- 作用: 对话输入区 —— 自动增高文本域、回车发送、发送/停止按钮 -->
<template>
  <div class="chat-composer">
    <div class="composer-box">
      <!-- 自动增高文本域 -->
      <textarea
        ref="inputRef"
        v-model="draft"
        class="composer-input"
        rows="1"
        :placeholder="placeholder"
        :disabled="disabled"
        @input="resize"
        @keydown="onKeydown"
      />
      <div class="composer-actions">
        <!-- 提示文案（回车发送/停止提示） -->
        <span class="composer-hint">{{ streaming ? '正在回复…' : hint }}</span>
        <!-- 流式回复中显示停止按钮 -->
        <button v-if="streaming" class="btn-stop" title="停止回复" @click="emit('stop')">
          <el-icon :size="16"><CircleClose /></el-icon>
          停止
        </button>
        <!-- 发送按钮 -->
        <button class="btn-send" :disabled="!draft.trim() || disabled" @click="submit">
          <el-icon :size="16"><Promotion /></el-icon>
          发送
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 对话输入组件
import { computed, nextTick, ref } from 'vue'
import { CircleClose, Promotion } from '@element-plus/icons-vue'

// 是否正在流式回复（此时禁用输入发送）
const props = withDefaults(
  defineProps<{
    streaming?: boolean
    disabled?: boolean
    enterToSend?: boolean
    placeholder?: string
  }>(),
  { streaming: false, disabled: false, enterToSend: true, placeholder: '输入消息，Enter 发送，Shift+Enter 换行' }
)

// 向外广播发送与停止事件
const emit = defineEmits<{
  (e: 'send', text: string): void
  (e: 'stop'): void
}>()

// 输入草稿
const draft = ref('')
// 文本域引用
const inputRef = ref<HTMLTextAreaElement | null>(null)

// 输入提示文案（随回车发送设置联动）
const hint = computed(() =>
  props.enterToSend ? 'Enter 发送 · Shift+Enter 换行' : 'Ctrl+Enter 发送 · Enter 换行'
)

// 文本域自适应高度
function resize(): void {
  const el = inputRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 140) + 'px'
}

// 键盘事件：Enter 发送、Shift+Enter 换行
function onKeydown(e: KeyboardEvent): void {
  if (props.enterToSend && e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
    e.preventDefault()
    submit()
  }
}

// 提交发送：清空草稿并复位高度
function submit(): void {
  const text = draft.value.trim()
  if (!text || props.streaming || props.disabled) return
  emit('send', text)
  draft.value = ''
  nextTick(() => {
    resize()
    inputRef.value?.focus()
  })
}

// 计算输入提示文案
function computedHint() {
  return props.enterToSend ? 'Enter 发送 · Shift+Enter 换行' : 'Ctrl+Enter 发送 · Enter 换行'
}
</script>

<style scoped>
/* 输入区外层留白 */
.chat-composer {
  padding: 12px 0 0;
}

/* 输入框容器：白底圆角卡片，聚焦时蓝色描边 */
.composer-box {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 12px 16px;
  transition: border-color var(--duration-base) var(--ease-out);
}
.composer-box:focus-within {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgb(37 99 235 / 0.1);
}

/* 文本域 */
.composer-input {
  display: block;
  width: 100%;
  max-height: 140px;
  border: none;
  outline: none;
  resize: none;
  font-family: inherit;
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  color: var(--color-text);
  background: transparent;
}
.composer-input:disabled {
  color: var(--color-text-muted);
  cursor: not-allowed;
}
.composer-input::placeholder {
  color: var(--color-text-muted);
}

/* 底部操作行 */
.composer-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}

/* 提示文案 */
.composer-hint {
  margin-right: auto;
  font-size: var(--font-size-caption);
  color: var(--color-text-muted);
}

/* 停止按钮 */
.btn-stop {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 32px;
  padding: 0 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  background: var(--color-surface);
  color: var(--color-text-secondary);
  font-size: var(--font-size-body);
  cursor: pointer;
  transition: all var(--duration-base) var(--ease-out);
}
.btn-stop:hover {
  color: var(--color-danger);
  border-color: var(--color-danger);
}

/* 发送按钮：黑色强调风格（与“使用”角标一致的反色语言） */
.btn-send {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 32px;
  padding: 0 16px;
  border: none;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  color: var(--color-text-invert);
  font-size: var(--font-size-body);
  cursor: pointer;
  transition: background var(--duration-base) var(--ease-out);
}
.btn-send:hover {
  background: var(--color-primary-hover);
}
.btn-send:disabled {
  background: #93c5fd;
  cursor: not-allowed;
}
</style>
