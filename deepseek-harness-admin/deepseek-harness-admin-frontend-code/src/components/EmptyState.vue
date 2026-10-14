<!--
---
last_updated: 2026-10-07
status: active          # active | deprecated | draft
owner: @qiang
---
文件职责：空状态占位。居中装饰图形 + 说明文案 + 可选引导按钮，
用于各列表页无数据时的统一兜底（对照 VISUAL_DESIGN.md 第 5 章 Empty 态）。
-->
<script setup lang="ts">
defineProps<{
  /** 空态说明文案 */
  description: string
  /** 引导按钮文案，不传则不展示按钮 */
  actionText?: string
}>()

const emit = defineEmits<{
  (e: 'action'): void
}>()
</script>

<template>
  <div class="empty-state">
    <div class="empty-icon" aria-hidden="true">
      <svg viewBox="0 0 48 48" width="56" height="56" fill="none">
        <path
          d="M14 12 L34 12 L40 18 L40 36 L14 36 Z"
          fill="url(#empty-grad)"
          stroke="none"
        />
        <path d="M14 12 L24 22 L34 12" stroke="#fff" stroke-width="2" fill="none" />
        <rect x="20" y="26" width="8" height="3" rx="1.5" fill="#fff" opacity="0.8" />
        <defs>
          <linearGradient id="empty-grad" x1="0" y1="0" x2="48" y2="48">
            <stop offset="0%" stop-color="#5B6BF5" />
            <stop offset="100%" stop-color="#8B7BFF" />
          </linearGradient>
        </defs>
      </svg>
    </div>
    <p class="empty-text">{{ description }}</p>
    <button v-if="actionText" type="button" class="btn-pill primary" @click="emit('action')">
      {{ actionText }}
    </button>
  </div>
</template>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-7) var(--space-5);
}

.empty-text {
  margin: 0;
  font-size: var(--font-size-body);
  color: var(--color-text-secondary);
}
</style>
