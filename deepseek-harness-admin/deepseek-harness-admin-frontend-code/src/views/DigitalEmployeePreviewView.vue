<!--
---
last_updated: 2026-10-07
status: active          # active | deprecated | draft
owner: @qiang
---
文件职责：数字员工预览页。左侧信息面板 + 右侧页签（预览/版本历史/配置）；
预览页签为对话验证（基于数字员工当前配置实时作答，含历史会话）；支持发布新版本（对照计划书 P3、VISUAL_DESIGN.md 4.5）。
说明：后端未提供发布记录列表接口，版本历史仅展示当前版本；回滚依赖历史记录，
待接口补齐后启用（docs/API_SPEC.md 第 5 章）。
-->
<script setup lang="ts">
// @ts-ignore - 解决 vue 模块声明文件未找到的类型检查问题
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Refresh } from '@element-plus/icons-vue'
import { fetchDigitalEmployee, publishDigitalEmployee } from '@/api/digitalEmployee'
import { fetchSkills } from '@/api/skill'
import { ApiError } from '@/api/apiClient'
import { formatDateTime } from '@/utils/format'
import type { DigitalEmployeeDTO, SkillDTO } from '@/types'
import ChatPreview from '@/components/ChatPreview.vue'
import SkeletonTable from '@/components/SkeletonTable.vue'

const route = useRoute()
const router = useRouter()
const employeeId = Number(route.params.id)

const employee = ref<DigitalEmployeeDTO | null>(null)
const skills = ref<SkillDTO[]>([])
const loading = ref(true)
const loadError = ref('')
const activeTab = ref('preview')
const publishing = ref(false)

/** 当前版本展示文案 */
const currentVersion = computed(() => employee.value?.publishedVersion ?? '未发布')

/** 解析 Skill 名称：id → 名称映射 */
const skillNames = computed(() => {
  const map = new Map(skills.value.map((s: SkillDTO) => [s.id, s]))
  return (employee.value?.skillIds ?? []).map((id: number) => map.get(id)).filter((s): s is SkillDTO => Boolean(s))
})

/** 模型配置摘要（后端自由 JSON，取约定字段） */
const modelSummary = computed(() => {
  const cfg = employee.value?.modelConfig ?? {}
  return {
    model: String(cfg.model ?? '-'),
    temperature: cfg.temperature ?? '-',
    maxTokens: cfg.maxTokens ?? '-'
  }
})

/** 加载详情与 Skill 名称映射 */
async function loadDetail() {
  loading.value = true
  loadError.value = ''
  try {
    const [emp, skillRes] = await Promise.all([
      fetchDigitalEmployee(employeeId),
      fetchSkills({ page: 1, size: 100 })
    ])
    employee.value = emp
    skills.value = skillRes.items
  } catch (e) {
    loadError.value = e instanceof Error ? e.message : '加载失败，请重试'
  } finally {
    loading.value = false
  }
}

/** 发布新版本：生成不可变配置快照与递增版本号 */
async function handlePublish() {
  if (publishing.value) return
  try {
    await ElMessageBox.confirm('发布将基于当前配置生成新版本，历史快照保持不变，是否继续？', '发布新版本', {
      confirmButtonText: '发布',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  publishing.value = true
  try {
    const release = await publishDigitalEmployee(employeeId)
    if (release.status === 'SUCCESS') {
      ElMessage.success(`发布成功，版本 ${release.version}`)
      await loadDetail()
    } else {
      ElMessage.error(release.failureReason || '发布未通过校验，请检查 Skill 及知识库状态')
    }
  } catch (e) {
    ElMessage.error(e instanceof ApiError ? e.message : '发布失败')
  } finally {
    publishing.value = false
  }
}

/** 回滚：需要历史发布记录列表支撑，当前后端未提供该接口，置灰并说明 */
function handleRollback() {
  ElMessage.warning('回滚需要发布历史记录支持，后端暂未提供版本列表接口，待补齐后启用')
}

watch(
  () => route.params.id,
  () => loadDetail()
)

onMounted(() => {
  loadDetail()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">数字员工预览</h1>
        <p class="page-subtitle">{{ employee?.name ?? '加载中…' }}</p>
      </div>
      <div class="page-actions">
        <button type="button" class="btn-pill" @click="router.push({ name: 'digital-employees' })">
          <el-icon :size="14"><ArrowLeft /></el-icon>
          返回列表
        </button>
        <button type="button" class="btn-pill primary" :loading="publishing" :disabled="publishing" @click="handlePublish">
          <el-icon :size="14"><Refresh /></el-icon>
          发布新版本
        </button>
      </div>
    </div>

    <SkeletonTable v-if="loading" :columns="3" :rows="6" />

    <div v-else-if="loadError" class="load-error">
      <p>{{ loadError }}</p>
      <button type="button" class="btn-pill primary" @click="loadDetail">重试</button>
    </div>

    <div v-else-if="employee" class="preview-layout">
      <!-- 左侧信息面板 -->
      <aside class="info-panel card">
        <div class="info-avatar">{{ employee.name.charAt(0) }}</div>
        <h2 class="info-name">{{ employee.name }}</h2>
        <p class="info-status">
          状态：{{ employee.status === 'PUBLISHED' ? '已发布' : employee.status === 'DRAFT' ? '草稿' : '已归档' }}
          <template v-if="employee.publishedVersion"> {{ employee.publishedVersion }}</template>
        </p>
        <p class="info-desc">{{ employee.description || '暂无描述' }}</p>

        <h3 class="info-label">关联 Skills</h3>
        <div class="skill-chips">
          <span v-for="s in skillNames" :key="s.id" class="skill-chip">
            {{ s.name }}（知识库 ×{{ s.knowledgeBaseIds.length }}）
          </span>
          <span v-if="skillNames.length === 0" class="info-muted">未关联 Skill</span>
        </div>

        <h3 class="info-label">模型配置</h3>
        <dl class="info-kv">
          <dt>模型</dt><dd>{{ modelSummary.model }}</dd>
          <dt>温度</dt><dd>{{ modelSummary.temperature }}</dd>
          <dt>上下文长度</dt><dd>{{ modelSummary.maxTokens }}</dd>
        </dl>

        <h3 class="info-label">版本历史</h3>
        <ul class="version-list">
          <li class="version-item current">
            <span class="version-no">{{ employee.publishedVersion ?? '-' }}</span>
            <span class="version-meta">{{ currentVersion === '未发布' ? '尚未发布' : '当前生效' }}</span>
          </li>
        </ul>
        <button type="button" class="link-btn rollback" disabled @click="handleRollback">回滚到历史版本（暂不可用）</button>
      </aside>

      <!-- 右侧页签区 -->
      <section class="preview-main card">
        <div class="tabs" role="tablist">
          <button
            v-for="tab in [
              { value: 'preview', label: '预览' },
              { value: 'history', label: '版本历史' },
              { value: 'config', label: '配置' }
            ]"
            :key="tab.value"
            type="button"
            class="tab"
            :class="{ active: activeTab === tab.value }"
            role="tab"
            :aria-selected="activeTab === tab.value"
            @click="activeTab = tab.value"
          >
            {{ tab.label }}
          </button>
        </div>

        <div v-show="activeTab === 'preview'" class="tab-body">
          <ChatPreview :employee-id="employee.id" :employee-name="employee.name" />
        </div>

        <div v-show="activeTab === 'history'" class="tab-body">
          <ul class="version-list large">
            <li class="version-item current">
              <span class="version-no">{{ employee.publishedVersion ?? '-' }}</span>
              <span class="version-meta">
                {{ currentVersion === '未发布' ? '尚未发布' : '当前生效' }} · {{ formatDateTime(employee.createdAt) }}
              </span>
            </li>
          </ul>
          <p class="tab-note">
            后端暂未提供发布记录列表接口，历史版本无法完整展示；回滚功能待接口补齐后启用。
          </p>
        </div>

        <div v-show="activeTab === 'config'" class="tab-body">
          <h3 class="info-label">系统提示词</h3>
          <pre class="prompt-box">{{ employee.systemPrompt }}</pre>
          <dl class="info-kv">
            <dt>模型</dt><dd>{{ modelSummary.model }}</dd>
            <dt>温度</dt><dd>{{ modelSummary.temperature }}</dd>
            <dt>上下文长度</dt><dd>{{ modelSummary.maxTokens }}</dd>
          </dl>
        </div>
      </section>
    </div>
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

.preview-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: var(--space-4);
  align-items: start;
}

.info-panel {
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  position: sticky;
  top: var(--space-4);
}

.info-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-accent-purple), var(--color-primary));
  color: #fff;
  font-size: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.info-name {
  margin: var(--space-1) 0 0;
  font-size: var(--font-size-card);
  font-weight: var(--font-weight-title);
}

.info-status {
  margin: 0;
  font-size: var(--font-size-body);
  color: var(--color-success);
}

.info-desc {
  margin: 0;
  font-size: var(--font-size-body);
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.info-label {
  margin: var(--space-2) 0 0;
  font-size: var(--font-size-body);
  font-weight: var(--font-weight-card);
}

.skill-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.skill-chip {
  font-size: var(--font-size-small);
  color: var(--color-primary);
  background: var(--color-primary-bg);
  padding: 2px 10px;
  border-radius: var(--radius-pill);
}

.info-muted {
  font-size: var(--font-size-small);
  color: var(--color-text-placeholder);
}

.info-kv {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 4px 12px;
  margin: 0;
  font-size: var(--font-size-body);
}

.info-kv dt {
  color: var(--color-text-secondary);
}

.info-kv dd {
  margin: 0;
  color: var(--color-text-main);
}

.version-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.version-list.large {
  max-height: 320px;
  overflow-y: auto;
}

.version-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-body);
}

.version-item.current {
  background: var(--color-primary-bg);
  border-color: var(--color-primary);
  border-left: 3px solid var(--color-primary);
}

.version-no {
  font-weight: var(--font-weight-card);
}

.version-meta {
  font-size: var(--font-size-small);
  color: var(--color-text-secondary);
}

.link-btn {
  border: none;
  background: none;
  font-size: var(--font-size-small);
  cursor: not-allowed;
  color: var(--color-text-placeholder);
  padding: 0;
  text-align: left;
}

.preview-main {
  min-height: 520px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.tabs {
  display: flex;
  gap: var(--space-4);
  padding: 0 var(--space-4);
  border-bottom: 1px solid var(--color-border-default);
}

.tab {
  border: none;
  background: none;
  font-size: var(--font-size-body);
  color: var(--color-text-secondary);
  padding: var(--space-2) 2px;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: var(--transition-base);
}

.tab.active {
  color: var(--color-text-main);
  font-weight: var(--font-weight-card);
  border-bottom-color: var(--color-primary);
}

.tab-body {
  flex: 1;
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
}

.prompt-box {
  margin: 0;
  padding: var(--space-3);
  background: var(--color-bg-page);
  border-radius: var(--radius-sm);
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: var(--font-size-body);
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.tab-note {
  font-size: var(--font-size-small);
  color: var(--color-text-placeholder);
}

@media (max-width: 1023px) {
  .preview-layout {
    grid-template-columns: 1fr;
  }
  .info-panel {
    position: static;
  }
}
</style>
