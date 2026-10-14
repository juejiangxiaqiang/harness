<!--
---
last_updated: 2026-10-08
status: active          # active | deprecated | draft
owner: @qiang
---
文件职责：胶囊式筛选标签组。激活态深色填充 + 白字，默认白色线框；
用于知识库状态筛选等场景（对照 VISUAL_DESIGN.md 4.2 交互样式）。
-->
<script setup lang="ts">
/** 单个筛选项 */
export interface CapsuleOption {
  value: string
  label: string
}

defineProps<{
  /** 筛选项列表 */
  options: CapsuleOption[]
  /** 当前选中值（受控 v-model） */
  modelValue: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

/** 切换筛选值 */
function select(value: string) {
  emit('update:modelValue', value)
}
</script>

<template>
  <div class="capsule-tabs" role="tablist" aria-label="筛选">
    <button
      v-for="opt in options"
      :key="opt.value"
      type="button"
      class="capsule"
      :class="{ active: modelValue === opt.value }"
      role="tab"
      :aria-selected="modelValue === opt.value"
      @click="select(opt.value)"
    >
      {{ opt.label }}
    </button>
  </div>
</template>

<style scoped>
/* 胶囊容器：细边框整体胶囊，内部选项无边框（对照 style.css .pill-tabs/.pill-tab） */
.capsule-tabs {
  display: inline-flex;
  gap: 0;
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-pill);
  background: var(--color-bg-surface);
  padding: 3px;
}

.capsule {
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  border-radius: var(--radius-pill);
  padding: 6px 18px;
  font-size: var(--font-size-body);
  cursor: pointer;
  transition: var(--transition-base);
  white-space: nowrap;
}

.capsule:hover {
  color: var(--color-text-main);
}

.capsule.active {
  background: var(--color-text-main);
  color: #fff;
  font-weight: var(--font-weight-card);
}
</style>
