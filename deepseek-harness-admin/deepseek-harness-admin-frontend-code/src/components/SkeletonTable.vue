<!--
---
last_updated: 2026-10-07
status: active          # active | deprecated | draft
owner: @qiang
---
文件职责：表格骨架屏。渲染 N 行 × M 列 shimmer 灰条，避免列表加载时闪白屏；
CSS 动画统一来自 assets/main.css（对照 VISUAL_DESIGN.md 第 5 章 Loading 态）。
-->
<script setup lang="ts">
defineProps<{
  /** 列数 */
  columns: number
  /** 行数 */
  rows: number
}>()
</script>

<template>
  <div class="skeleton-table" role="status" aria-label="加载中">
    <div class="skeleton-row header">
      <div v-for="i in columns" :key="`h-${i}`" class="skeleton-cell skeleton-shimmer" />
    </div>
    <div v-for="r in rows" :key="r" class="skeleton-row">
      <div v-for="c in columns" :key="`${r}-${c}`" class="skeleton-cell skeleton-shimmer" />
    </div>
  </div>
</template>

<style scoped>
.skeleton-table {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-md);
  padding: var(--space-3);
}

.skeleton-row {
  display: flex;
  gap: var(--space-3);
  padding: var(--space-2) 0;
}

.skeleton-row.header {
  padding-bottom: var(--space-3);
  border-bottom: 1px solid var(--color-border-default);
}

.skeleton-cell {
  height: 16px;
  flex: 1;
}
</style>
