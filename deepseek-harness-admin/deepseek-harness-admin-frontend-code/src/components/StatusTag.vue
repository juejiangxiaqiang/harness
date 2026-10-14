<!--
---
last_updated: 2026-10-07
status: active          # active | deprecated | draft
owner: @qiang
---
文件职责：知识库状态标签。UPLOADING/PROCESSING/READY/FAILED 映射配色与文案，
PROCESSING 带旋转小菊花；失败原因经 Tooltip 展示（对照 VISUAL_DESIGN.md 4.2）。
-->
<script setup lang="ts">
import type { KnowledgeBaseStatus } from '@/types'

/** 状态标签配置：文案与语义色，深色文字 + 浅色底满足对比度 */
const STATUS_MAP: Record<KnowledgeBaseStatus, { label: string; cls: string }> = {
  UPLOADING: { label: '待处理', cls: 'default' },
  PROCESSING: { label: '处理中', cls: 'info' },
  READY: { label: '已就绪', cls: 'success' },
  FAILED: { label: '失败', cls: 'danger' }
}

const props = defineProps<{
  /** 知识库状态枚举 */
  status: KnowledgeBaseStatus
  /** 失败原因，仅 FAILED 展示 Tooltip */
  failureReason?: string | null
  /** 处理进度（0-100），仅 PROCESSING 展示 */
  progress?: number
}>()

const current = () => STATUS_MAP[props.status] ?? STATUS_MAP.FAILED
</script>

<template>
  <span class="status-tag" :class="`status-${current().cls}`">
    <span v-if="status === 'PROCESSING'" class="spin-dot" />
    <el-tooltip v-if="status === 'FAILED' && failureReason" :content="failureReason" placement="top">
      <span class="status-text">{{ current().label }}</span>
    </el-tooltip>
    <span v-else class="status-text">{{ current().label }}</span>
    <span v-if="status === 'PROCESSING' && typeof progress === 'number'" class="status-progress">
      {{ progress }}%
    </span>
  </span>
</template>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: var(--radius-pill);
  font-size: var(--font-size-small);
  white-space: nowrap;
}

.status-default {
  color: var(--color-text-secondary);
  background: #f5f5f5;
}

.status-info {
  color: var(--color-info);
  background: var(--color-info-bg);
}

.status-success {
  color: var(--color-success);
  background: var(--color-success-bg);
}

.status-danger {
  color: var(--color-danger);
  background: var(--color-danger-bg);
}

.status-progress {
  font-size: var(--font-size-small);
  color: var(--color-text-secondary);
}

/* PROCESSING 旋转小菊花 */
.spin-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  border: 2px solid var(--color-info);
  border-top-color: transparent;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .spin-dot {
    animation: none;
  }
}
</style>
