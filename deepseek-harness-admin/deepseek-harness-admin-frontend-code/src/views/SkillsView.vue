<!--
---
last_updated: 2026-10-08
status: active          # active | deprecated | draft
owner: @qiang
---
文件职责：Skills 列表页。服务端分页展示；新建入口跳转创建页
（对照计划书 P2 与 VISUAL_DESIGN.md 4.3）。
-->
<script setup lang="ts">
// @ts-ignore - 解决 vue 模块声明文件未找到的类型检查问题
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { fetchSkills } from '@/api/skill'
import { formatDateTime } from '@/utils/format'
import type { SkillDTO } from '@/types'
import SkeletonTable from '@/components/SkeletonTable.vue'
import EmptyState from '@/components/EmptyState.vue'

/** 服务端分页参数：page 从 1 开始，size 单页上限 100 */
const PAGE_SIZE = 10

const router = useRouter()
const items = ref<SkillDTO[]>([])
const total = ref(0)
const currentPage = ref(1)
const loading = ref(true)
const loadError = ref('')

/** 加载当前页数据（服务端分页，Skills 无客户端筛选需求） */
async function loadList(page = currentPage.value) {
  loading.value = true
  loadError.value = ''
  try {
    const res = await fetchSkills({ page, size: PAGE_SIZE })
    items.value = res.items
    total.value = res.total
    currentPage.value = res.page
  } catch (e) {
    loadError.value = e instanceof Error ? e.message : '加载失败，请重试'
  } finally {
    loading.value = false
  }
}

/** 跳转创建页 */
function goCreate() {
  router.push({ name: 'skill-create' })
}

onMounted(() => {
  loadList()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">Skills</h1>
        <p class="page-subtitle">管理全局共享 Skill，关联 READY 状态知识库</p>
      </div>
      <div class="page-actions">
        <button type="button" class="btn-pill primary" @click="goCreate">
          <el-icon :size="14"><Plus /></el-icon>
          新建 Skill
        </button>
      </div>
    </div>

    <SkeletonTable v-if="loading" :columns="5" :rows="6" />

    <div v-else-if="loadError" class="load-error">
      <p>{{ loadError }}</p>
      <button type="button" class="btn-pill primary" @click="loadList()">重试</button>
    </div>

    <EmptyState
      v-else-if="items.length === 0"
      description="暂无 Skill，点击新建 Skill 创建并关联知识库"
      action-text="新建 Skill"
      @action="goCreate"
    />

    <template v-else>
      <div class="card table-card">
        <table class="data-table">
          <thead>
            <tr>
              <th>名称</th>
              <th>描述</th>
              <th>状态</th>
              <th>关联知识库</th>
              <th>版本</th>
              <th>创建时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in items" :key="item.id">
              <td class="cell-name">{{ item.name }}</td>
              <td class="cell-desc">{{ item.description || '-' }}</td>
              <td>
                <span class="skill-status" :class="item.status === 'ACTIVE' ? 'on' : 'off'">
                  {{ item.status === 'ACTIVE' ? '启用' : '停用' }}
                </span>
              </td>
              <td class="cell-muted">{{ item.knowledgeBaseIds.length }}</td>
              <td class="cell-muted">{{ item.version }}</td>
              <td class="cell-muted">{{ formatDateTime(item.createdAt) }}</td>
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
        @current-change="loadList"
      />
    </template>
  </div>
</template>

<style scoped>
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

.cell-desc {
  color: var(--color-text-secondary);
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cell-muted {
  color: var(--color-text-secondary);
  font-size: var(--font-size-small);
}

.skill-status {
  padding: 2px 8px;
  border-radius: var(--radius-pill);
  font-size: var(--font-size-small);
}

.skill-status.on {
  color: var(--color-success);
  background: var(--color-success-bg);
}

.skill-status.off {
  color: var(--color-text-secondary);
  background: #f5f5f5;
}

.pager {
  margin-top: var(--space-3);
  justify-content: flex-end;
}
</style>
