<!--
---
last_updated: 2026-10-08
status: active          # active | deprecated | draft
owner: @qiang
---
文件职责：知识库列表页。状态筛选、名称搜索、上传弹窗、重新向量化、分页；
对 PROCESSING/UPLOADING 条目轮询刷新状态（对照计划书 P2 与 VISUAL_DESIGN.md 4.2）。
说明：后端列表仅支持 page/size，状态与名称筛选在本地完成；后端无文件大小字段，
以"分片数"替代展示。删除操作因后端未提供 DELETE 接口暂未实现（计划书与 API 有差异，
见 docs/API_SPEC.md 第 3 章）。
-->
<script setup lang="ts">
// @ts-ignore - 解决 vue 模块声明文件未找到的类型检查问题
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { fetchKnowledgeBases, reprocessKnowledgeBase } from '@/api/knowledgeBase'
import { extractExtension, formatDateTime } from '@/utils/format'
import type { KnowledgeBaseDTO } from '@/types'
import StatusTag from '@/components/StatusTag.vue'
import CapsuleTabs from '@/components/CapsuleTabs.vue'
import SkeletonTable from '@/components/SkeletonTable.vue'
import EmptyState from '@/components/EmptyState.vue'
import UploadKnowledgeDialog from '@/components/UploadKnowledgeDialog.vue'

/** 本地分页每页条数 */
const PAGE_SIZE = 10

const allItems = ref<KnowledgeBaseDTO[]>([])
const loading = ref(true)
const loadError = ref('')
const statusFilter = ref('ALL')
const keyword = ref('')
const currentPage = ref(1)
const uploadVisible = ref(false)

/** 状态筛选选项：值对齐后端枚举 */
const statusOptions = [
  { value: 'ALL', label: '全部' },
  { value: 'UPLOADING', label: '待处理' },
  { value: 'PROCESSING', label: '处理中' },
  { value: 'READY', label: '已就绪' },
  { value: 'FAILED', label: '失败' }
]

/** 本地筛选：状态 + 名称/文件名关键字 */
const filteredItems = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return allItems.value.filter((it: KnowledgeBaseDTO) => {
    if (statusFilter.value !== 'ALL' && it.status !== statusFilter.value) return false
    if (kw && !it.name.toLowerCase().includes(kw) && !it.fileName.toLowerCase().includes(kw)) return false
    return true
  })
})

const total = computed(() => filteredItems.value.length)

/** 当前页数据切片 */
const pagedItems = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return filteredItems.value.slice(start, start + PAGE_SIZE)
})

/** 是否存在进行中任务：驱动轮询开关 */
const hasProcessing = computed(() =>
  allItems.value.some((it: KnowledgeBaseDTO) => it.status === 'UPLOADING' || it.status === 'PROCESSING')
)

/**
 * 加载列表：一次取回单页上限（管理后台知识库规模适用），
 * 分页与筛选在本地进行（后端列表不支持状态/名称参数）
 */
async function loadList() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await fetchKnowledgeBases({ page: 1, size: 100 })
    allItems.value = res.items
  } catch (e) {
    loadError.value = e instanceof Error ? e.message : '加载失败，请重试'
  } finally {
    loading.value = false
  }
}

/** 轮询定时器：仅在有进行中任务时刷新，避免无谓请求 */
let pollTimer: number | null = null

function startPolling() {
  stopPolling()
  pollTimer = window.setInterval(() => {
    if (hasProcessing.value) {
      loadList()
    }
  }, 4000)
}

function stopPolling() {
  if (pollTimer !== null) {
    window.clearInterval(pollTimer)
    pollTimer = null
  }
}

/** 重新向量化：清理旧向量后重建，仅 READY/FAILED 状态可触发 */
async function handleReprocess(item: KnowledgeBaseDTO) {
  try {
    await ElMessageBox.confirm(
      `重新处理「${item.name}」将清理旧向量后重新向量化，是否继续？`,
      '重新向量化',
      { confirmButtonText: '重新处理', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await reprocessKnowledgeBase(item.id)
    ElMessage.success('已触发重新向量化')
    loadList()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

/** 上传完成回调：刷新列表并进入轮询 */
function onUploaded() {
  loadList()
}

onMounted(() => {
  loadList()
  startPolling()
})

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">知识库</h1>
        <p class="page-subtitle">管理全局共享知识库，上传后自动解析、向量化</p>
      </div>
      <div class="page-actions">
        <el-input
          v-model="keyword"
          class="search-input"
          placeholder="搜索知识库名称"
          clearable
          :prefix-icon="Search"
        />
        <button type="button" class="btn-pill" @click="loadList">
          <el-icon :size="14"><Refresh /></el-icon>
          刷新
        </button>
        <button type="button" class="btn-pill primary" @click="uploadVisible = true">
          <el-icon :size="14"><Plus /></el-icon>
          上传知识库
        </button>
      </div>
    </div>

    <CapsuleTabs v-model="statusFilter" :options="statusOptions" class="filter-bar" />

    <SkeletonTable v-if="loading" :columns="6" :rows="6" />

    <div v-else-if="loadError" class="load-error">
      <p>{{ loadError }}</p>
      <button type="button" class="btn-pill primary" @click="loadList">重试</button>
    </div>

    <EmptyState
      v-else-if="filteredItems.length === 0"
      description="暂无知识库，上传文档后自动解析与向量化"
      action-text="去上传"
      @action="uploadVisible = true"
    />

    <template v-else>
      <div class="card table-card">
        <table class="data-table">
          <thead>
            <tr>
              <th>名称</th>
              <th>类型</th>
              <th>分片数</th>
              <th>状态</th>
              <th>创建时间</th>
              <th class="col-actions">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in pagedItems" :key="item.id">
              <td>
                <div class="cell-name">{{ item.name }}</div>
                <div class="cell-sub">{{ item.fileName }}</div>
              </td>
              <td class="cell-muted">{{ extractExtension(item.fileName) || '-' }}</td>
              <td class="cell-muted">{{ item.chunkCount }}</td>
              <td>
                <StatusTag :status="item.status" :failure-reason="item.failureReason" />
              </td>
              <td class="cell-muted">{{ formatDateTime(item.createdAt) }}</td>
              <td class="col-actions">
                <button
                  v-if="item.status === 'READY' || item.status === 'FAILED'"
                  type="button"
                  class="link-btn"
                  @click="handleReprocess(item)"
                >
                  重新向量化
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <el-pagination
        v-if="total > PAGE_SIZE"
        class="pager"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="PAGE_SIZE"
        :current-page="currentPage"
        @current-change="(p: number) => (currentPage = p)"
      />
    </template>

    <UploadKnowledgeDialog
      v-model:visible="uploadVisible"
      @uploaded="onUploaded"
    />
  </div>
</template>

<style scoped>
.filter-bar {
  margin-bottom: var(--space-3);
}

.search-input {
  width: 220px;
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

.table-card {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--font-size-body);
}

.data-table th {
  text-align: left;
  padding: 10px var(--space-3);
  background: var(--color-bg-row-hover);
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-body);
  font-size: var(--font-size-small);
  border-bottom: 1px solid var(--color-border-default);
  white-space: nowrap;
}

.data-table td {
  padding: var(--space-2) var(--space-3);
  border-bottom: 1px solid var(--color-border-default);
  vertical-align: middle;
}

.data-table tbody tr:hover {
  background: var(--color-bg-row-hover);
}

.data-table tr:last-child td {
  border-bottom: none;
}

.cell-name {
  color: var(--color-text-main);
  font-weight: var(--font-weight-card);
}

.cell-sub {
  font-size: var(--font-size-small);
  color: var(--color-text-placeholder);
  margin-top: 2px;
}

.cell-muted {
  color: var(--color-text-secondary);
  font-size: var(--font-size-small);
}

.col-actions {
  text-align: right;
  white-space: nowrap;
}

.link-btn {
  border: none;
  background: none;
  color: var(--color-primary);
  font-size: var(--font-size-small);
  cursor: pointer;
  padding: 0;
}

.link-btn:hover {
  text-decoration: underline;
  color: var(--color-primary-hover);
}

.pager {
  margin-top: var(--space-3);
  justify-content: flex-end;
}
</style>
