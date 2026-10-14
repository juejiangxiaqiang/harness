<!--
---
last_updated: 2026-10-08
status: active          # active | deprecated | draft
owner: @qiang
---
文件职责：通用卡片式多选面板。支持本地搜索、勾选计数、禁用项与原因提示；
创建 Skill 时选择 READY 知识库、创建数字员工时选择 Skill 均复用本组件
（对照 VISUAL_DESIGN.md 4.3/4.4 卡片多选交互）。
-->
<script setup lang="ts">
// @ts-ignore - 解决 vue 模块声明文件未找到的类型检查问题
import { computed, ref } from 'vue'
import { Lock } from '@element-plus/icons-vue'

/** 面板条目：disabled 项不可勾选，hover/点击展示 disabledTip */
export interface SelectItem {
  id: number
  label: string
  /** 次级说明，如 "关联知识库 ×3" */
  subtitle?: string
  /** 右上角状态小标签 */
  tag?: string
  /** 圆形渐变图标首字，缺省取 label 首字 */
  icon?: string
  disabled?: boolean
  disabledTip?: string
}

const props = withDefaults(
  defineProps<{
    /** 面板标题 */
    title: string
    /** 可选项列表 */
    items: SelectItem[]
    /** 已选 ID 列表（受控 v-model） */
    modelValue: number[]
    /** 是否启用本地搜索过滤，默认开启 */
    searchable?: boolean
  }>(),
  { searchable: true }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: number[]): void
}>()

/** 搜索关键字 */
const keyword = ref('')

/** 已选 ID 集合，便于 O(1) 判断 */
const selectedSet = computed(() => new Set(props.modelValue))

/** 本地模糊过滤：按名称或副标题匹配 */
const filteredItems = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return props.items
  return props.items.filter(
    (it) => it.label.toLowerCase().includes(kw) || (it.subtitle ?? '').toLowerCase().includes(kw)
  )
})

/** 勾选切换：禁用项直接忽略 */
function toggle(item: SelectItem) {
  if (item.disabled) return
  const next = new Set(props.modelValue)
  if (next.has(item.id)) {
    next.delete(item.id)
  } else {
    next.add(item.id)
  }
  emit('update:modelValue', Array.from(next))
}
</script>

<template>
  <div class="panel card">
    <div class="panel-head">
      <h3 class="panel-title">{{ title }}</h3>
      <span class="panel-count">已选 {{ modelValue.length }}/{{ items.length }}</span>
    </div>
    <el-input
      v-if="searchable !== false"
      v-model="keyword"
      class="panel-search"
      placeholder="搜索名称"
      clearable
    />
    <ul class="panel-list">
      <li
        v-for="item in filteredItems"
        :key="item.id"
        class="panel-item"
        :class="{ selected: selectedSet.has(item.id), disabled: item.disabled }"
        role="checkbox"
        :aria-checked="selectedSet.has(item.id)"
        :tabindex="item.disabled ? -1 : 0"
        @click="toggle(item)"
        @keydown.enter="toggle(item)"
        @keydown.space.prevent="toggle(item)"
      >
        <div class="item-icon">{{ item.icon || item.label.charAt(0) }}</div>
        <div class="item-main">
          <div class="item-label">{{ item.label }}</div>
          <div v-if="item.subtitle" class="item-subtitle">{{ item.subtitle }}</div>
        </div>
        <span v-if="item.tag" class="item-tag">{{ item.tag }}</span>
        <span v-if="item.disabled" class="item-lock">
          <el-tooltip :content="item.disabledTip || '当前不可选择'" placement="top">
            <el-icon :size="14"><Lock /></el-icon>
          </el-tooltip>
        </span>
        <span class="item-check" aria-hidden="true">
          <span v-if="selectedSet.has(item.id)" class="check-mark">✓</span>
        </span>
      </li>
      <li v-if="filteredItems.length === 0" class="panel-empty">无匹配项</li>
    </ul>
  </div>
</template>

<style scoped>
.panel {
  padding: var(--space-3);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-title {
  margin: 0;
  font-size: var(--font-size-card);
  font-weight: var(--font-weight-card);
}

.panel-count {
  font-size: var(--font-size-small);
  color: var(--color-primary);
  background: var(--color-primary-bg);
  border-radius: var(--radius-pill);
  padding: 2px 10px;
}

.panel-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 360px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.panel-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: 10px;
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: var(--transition-base);
}

.panel-item:hover {
  background: var(--color-bg-page);
  border-color: var(--color-primary);
}

.panel-item.selected {
  border-color: var(--color-primary);
  border-width: 2px;
  padding: 9px;
  background: var(--color-primary-bg);
}

.panel-item.disabled {
  opacity: 0.55;
  cursor: not-allowed;
  background: #fafafa;
}

.item-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--color-accent-blue), var(--color-accent-teal));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-body);
}

.item-main {
  flex: 1;
  min-width: 0;
}

.item-label {
  font-size: var(--font-size-body);
  color: var(--color-text-main);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-subtitle {
  font-size: var(--font-size-small);
  color: var(--color-text-secondary);
  margin-top: 2px;
}

.item-tag {
  font-size: var(--font-size-small);
  color: var(--color-text-secondary);
  background: #f5f5f5;
  padding: 2px 8px;
  border-radius: var(--radius-pill);
}

.item-lock {
  display: inline-flex;
  color: var(--color-text-placeholder);
}

.item-check {
  width: 18px;
  height: 18px;
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-sm);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 12px;
  color: #fff;
}

.panel-item.selected .item-check {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.panel-empty {
  text-align: center;
  color: var(--color-text-placeholder);
  padding: var(--space-3);
  font-size: var(--font-size-small);
}
</style>
