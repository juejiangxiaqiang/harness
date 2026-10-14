<!--
---
last_updated: 2026-10-08
status: active          # active | deprecated | draft
owner: @qiang
---
文件职责：数字员工列表页。卡片网格 + 状态筛选（DRAFT/PUBLISHED/ARCHIVED），
点击卡片跳转预览页（对照计划书 P3 与 VISUAL_DESIGN.md 4.4）。
说明：后端列表仅支持 page/size，状态筛选在本地完成；数字员工表无业务分类字段，
不提供分类筛选（与 P3 计划书备注一致）。
-->
<script setup lang="ts">
// @ts-ignore - 解决 vue 模块声明文件未找到的类型检查问题
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { fetchDigitalEmployees } from '@/api/digitalEmployee'
import type { DigitalEmployeeDTO } from '@/types'
import CapsuleTabs from '@/components/CapsuleTabs.vue'
import SkeletonTable from '@/components/SkeletonTable.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()

const allItems = ref<DigitalEmployeeDTO[]>([])
const loading = ref(true)
const loadError = ref('')
const statusFilter = ref('ALL')

/** 状态筛选选项：值对齐后端枚举 */
const statusOptions = [
  { value: 'ALL', label: '全部' },
  { value: 'DRAFT', label: '草稿' },
  { value: 'PUBLISHED', label: '已发布' },
  { value: 'ARCHIVED', label: '已归档' }
]

/** 本地状态筛选后的卡片集合 */
const filteredItems = computed(() => {
  if (statusFilter.value === 'ALL') return allItems.value
  return allItems.value.filter((it: DigitalEmployeeDTO) => it.status === statusFilter.value)
})

/** 发布状态文案映射 */
const statusText: Record<DigitalEmployeeDTO['status'], string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  ARCHIVED: '已归档'
}

/** 卡片圆形图标渐变池：按业务域取色（对照 style.css .round-icon-blue/purple/teal） */
const GRADIENT_POOL = [
  'linear-gradient(135deg, var(--color-primary), var(--color-accent-blue))',
  'linear-gradient(135deg, var(--color-accent-purple), var(--color-primary))',
  'linear-gradient(135deg, var(--color-accent-teal), #3bae7d)'
]

/** 按索引取图标渐变，保证同域卡片配色稳定 */
function iconGradient(index: number): string {
  return GRADIENT_POOL[index % GRADIENT_POOL.length]
}

/** 加载列表：一次取回单页上限，筛选在本地进行 */
async function loadList() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await fetchDigitalEmployees({ page: 1, size: 100 })
    allItems.value = res.items
  } catch (e) {
    loadError.value = e instanceof Error ? e.message : '加载失败，请重试'
  } finally {
    loading.value = false
  }
}

/** 跳转数字员工预览页 */
function goPreview(item: DigitalEmployeeDTO) {
  router.push({ name: 'employee-preview', params: { id: item.id } })
}

onMounted(() => {
  loadList()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">数字员工</h1>
        <p class="page-subtitle">管理全局共享数字员工，发布后供普通用户使用</p>
      </div>
      <div class="page-actions">
        <button type="button" class="btn-pill primary" @click="router.push({ name: 'employee-create' })">
          <el-icon :size="14"><Plus /></el-icon>
          新建数字员工
        </button>
      </div>
    </div>

    <CapsuleTabs v-model="statusFilter" :options="statusOptions" class="filter-bar" />

    <SkeletonTable v-if="loading" :columns="4" :rows="6" />

    <div v-else-if="loadError" class="load-error">
      <p>{{ loadError }}</p>
      <button type="button" class="btn-pill primary" @click="loadList">重试</button>
    </div>

    <EmptyState
      v-else-if="filteredItems.length === 0"
      description="暂无数字员工，点击新建数字员工创建"
      action-text="新建数字员工"
      @action="router.push({ name: 'employee-create' })"
    />

    <template v-else>
      <!-- 结果计数：灰色小字（对照 design/employees.html countText） -->
      <p class="result-count">共 {{ filteredItems.length }} 个数字员工</p>
      <div class="card-grid">
        <article
          v-for="(item, idx) in filteredItems"
          :key="item.id"
          class="emp-card"
          role="button"
          tabindex="0"
          @click="goPreview(item)"
          @keydown.enter="goPreview(item)"
        >
          <div class="emp-top">
            <div class="round-icon" :style="{ background: iconGradient(idx) }">{{ item.name.charAt(0) }}</div>
            <span class="emp-status" :class="item.status.toLowerCase()">
              {{ statusText[item.status] }}{{ item.publishedVersion ? ` ${item.publishedVersion}` : '' }}
            </span>
          </div>
          <h3 class="emp-name">{{ item.name }}</h3>
          <p class="emp-desc">{{ item.description || '暂无描述' }}</p>
          <div class="emp-meta">
            <span>技能 ×{{ item.skillIds.length }}</span>
            <span class="emp-view">查看详情 →</span>
          </div>
        </article>
      </div>
    </template>
  </div>
</template>

<style scoped>
.filter-bar {
  margin-bottom: var(--space-3);
}

.load-error {
  padding: var(--space-6);
  text-align: center;
  color: var(--color-danger);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
}

/* 结果计数：灰色小字（对照 style.css .form-hint） */
.result-count {
  margin: 0 0 var(--space-2);
  font-size: var(--font-size-small);
  color: var(--color-text-placeholder);
}

/* 卡片网格：内容区 ≈1320px 时基准 4 列，逐级降列（对照 style.css grid-4 断点） */
.card-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-3);
}

.emp-card {
  display: flex;
  flex-direction: column;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-md);
  padding: var(--space-4);
  cursor: pointer;
  transition: var(--transition-base);
}

/* 悬停：主色边框 + 卡片阴影 + 轻微上移，暗示可点击（对照 style.css .emp-card:hover） */
.emp-card:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-card);
  transform: translateY(-2px);
}

.emp-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-1);
}

/* 圆形渐变文字图标：40px 渐变底 + 白字（对照 style.css .round-icon） */
.round-icon {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: var(--font-size-card);
  font-weight: var(--font-weight-card);
}

.emp-status {
  padding: 2px 10px;
  border-radius: var(--radius-pill);
  font-size: var(--font-size-small);
  white-space: nowrap;
}

.emp-status.published {
  color: var(--color-success);
  background: var(--color-success-bg);
}

.emp-status.draft {
  color: var(--color-text-secondary);
  background: var(--color-bg-tag-gray);
}

.emp-status.archived {
  color: var(--color-text-placeholder);
  background: var(--color-bg-tag-gray);
}

.emp-name {
  margin: var(--space-2) 0 0;
  font-size: var(--font-size-card);
  font-weight: var(--font-weight-card);
  color: var(--color-text-main);
}

/* 描述固定两行省略，保证卡片高度统一（对照 style.css .emp-card-desc） */
.emp-desc {
  margin: 6px 0 0;
  font-size: var(--font-size-small);
  color: var(--color-text-secondary);
  line-height: 1.6;
  min-height: 38px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 底部元信息：上虚线分隔 + 毕间距对齐设计稿 */
.emp-meta {
  margin-top: var(--space-3);
  padding-top: var(--space-2);
  border-top: 1px dashed var(--color-border-default);
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: var(--font-size-small);
  color: var(--color-text-secondary);
}

.emp-view {
  color: var(--color-primary);
  font-weight: var(--font-weight-card);
}

.emp-card:hover .emp-view {
  text-decoration: underline;
}

/* 响应式：列数随断点降级（对照 style.css 响应式断点） */
@media (max-width: 1200px) {
  .card-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 992px) {
  .card-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .card-grid {
    grid-template-columns: 1fr;
  }
}
</style>
