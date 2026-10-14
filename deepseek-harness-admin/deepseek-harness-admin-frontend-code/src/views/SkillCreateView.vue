<!--
---
last_updated: 2026-10-07
status: active          # active | deprecated | draft
owner: @qiang
---
文件职责：创建 Skill 页。左侧表单（名称/描述/内容配置）+ 右侧 READY 知识库
卡片多选；仅 READY 状态可选，失焦校验与提交全量校验（对照计划书 P2、
VISUAL_DESIGN.md 4.3 与 API_SPEC.md 第 4 章）。
-->
<script setup lang="ts">
// @ts-ignore - 解决 vue 模块声明文件未找到的类型检查问题
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchKnowledgeBases } from '@/api/knowledgeBase'
import { createSkill } from '@/api/skill'
import { ApiError } from '@/api/apiClient'
import type { KnowledgeBaseDTO } from '@/types'
import CardSelectPanel, { type SelectItem } from '@/components/CardSelectPanel.vue'
import SkeletonTable from '@/components/SkeletonTable.vue'

const router = useRouter()

/** 表单字段：name 必填 ≤128；content 必填 ≤10000；描述 ≤500 */
const form = ref({ name: '', description: '', content: '' })
const fieldErrors = ref<Record<string, string>>({})
const submitError = ref('')
const submitting = ref(false)

/** 知识库候选与已选 ID */
const kbItems = ref<SelectItem[]>([])
const selectedIds = ref<number[]>([])
const kbLoading = ref(true)

/**
 * 加载知识库候选：非 READY 状态禁用并提示原因（KNOWLEDGE_BASE_NOT_READY）
 */
async function loadKnowledgeBases() {
  kbLoading.value = true
  try {
    const res = await fetchKnowledgeBases({ page: 1, size: 100 })
    kbItems.value = res.items.map((kb: KnowledgeBaseDTO) => ({
      id: kb.id,
      label: kb.name,
      subtitle: kb.fileName,
      tag: kb.status === 'READY' ? 'READY' : kb.status,
      icon: kb.name.charAt(0),
      disabled: kb.status !== 'READY',
      disabledTip: '仅 READY 状态知识库可关联'
    }))
  } finally {
    kbLoading.value = false
  }
}

/** 字段级校验：返回是否通过，并写入内联错误 */
function validateField(key: 'name' | 'description' | 'content'): boolean {
  const errors: Record<string, string> = { ...fieldErrors.value }
  const value = form.value[key]
  if (key === 'name') {
    if (!value.trim()) errors[key] = '名称必填'
    else if (value.length > 128) errors[key] = '名称不能超过 128 个字符'
    else delete errors[key]
  } else if (key === 'description') {
    if (value.length > 500) errors[key] = '描述不能超过 500 个字符'
    else delete errors[key]
  } else if (key === 'content') {
    if (!value.trim()) errors[key] = '内容配置必填'
    else if (value.length > 10000) errors[key] = '内容不能超过 10000 个字符'
    else delete errors[key]
  }
  fieldErrors.value = errors
  return !errors[key]
}

/** 失焦校验 */
function onBlur(key: 'name' | 'description' | 'content') {
  validateField(key)
}

/** 全量校验：字段 + 至少选择一个知识库 */
function validateAll(): boolean {
  const ok = [validateField('name'), validateField('description'), validateField('content')].every(Boolean)
  if (selectedIds.value.length === 0) {
    submitError.value = '请至少选择一个 READY 状态的知识库'
    return false
  }
  return ok
}

/** 提交创建：成功后回列表，失败内联展示后端原因 */
async function handleSubmit() {
  if (submitting.value) return
  submitError.value = ''
  if (!validateAll()) return
  submitting.value = true
  try {
    await createSkill({
      name: form.value.name.trim(),
      description: form.value.description.trim(),
      content: form.value.content.trim(),
      knowledgeBaseIds: selectedIds.value
    })
    ElMessage.success('Skill 创建成功')
    router.push({ name: 'skills' })
  } catch (e) {
    submitError.value = e instanceof ApiError ? e.message : '创建失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadKnowledgeBases()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">创建 Skill</h1>
        <p class="page-subtitle">填写内容配置并关联 READY 状态知识库</p>
      </div>
    </div>

    <div class="create-layout">
      <div class="card form-card">
        <!-- 基本信息分组（对照 style.css .form-card-title） -->
        <h2 class="group-title">基本信息</h2>

        <label class="field">
          <span class="field-label">名称 <em class="required">*</em></span>
          <input
            v-model="form.name"
            class="field-input"
            type="text"
            placeholder="Skill 名称"
            @blur="onBlur('name')"
          />
          <span v-if="fieldErrors.name" class="field-error">{{ fieldErrors.name }}</span>
        </label>

        <label class="field">
          <span class="field-label">描述</span>
          <textarea
            v-model="form.description"
            class="field-textarea"
            rows="3"
            placeholder="Skill 用途说明（选填）"
            @blur="onBlur('description')"
          />
          <span v-if="fieldErrors.description" class="field-error">{{ fieldErrors.description }}</span>
        </label>

        <!-- 内容配置分组（对照 style.css 内容配置） -->
        <h2 class="group-title">内容配置</h2>

        <label class="field">
          <span class="field-label">内容配置 <em class="required">*</em></span>
          <textarea
            v-model="form.content"
            class="field-textarea mono"
            rows="8"
            placeholder="提示词模板 / 内容配置"
            @blur="onBlur('content')"
          />
          <span v-if="fieldErrors.content" class="field-error">{{ fieldErrors.content }}</span>
        </label>

        <p v-if="submitError" class="submit-error" role="alert">{{ submitError }}</p>

        <div class="form-actions">
          <button type="button" class="btn-pill" @click="router.push({ name: 'skills' })">取消</button>
          <button type="button" class="btn-pill primary" :disabled="submitting" @click="handleSubmit">
            {{ submitting ? '保存中…' : '保存' }}
          </button>
        </div>
      </div>

      <SkeletonTable v-if="kbLoading" :columns="1" :rows="5" />
      <CardSelectPanel
        v-else
        v-model="selectedIds"
        title="关联知识库"
        :items="kbItems"
        class="panel-wrap"
      />
    </div>
  </div>
</template>

<style scoped>
.create-layout {
  display: grid;
  grid-template-columns: 3fr 2fr;
  gap: var(--space-4);
  align-items: start;
}

.form-card {
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

/* 分组标题（对照 style.css .form-card-title） */
.group-title {
  margin: 8px 0 0;
  font-size: var(--font-size-card);
  font-weight: var(--font-weight-card);
  color: var(--color-text-main);
}

.group-title + .field {
  margin-top: var(--space-2);
}

.field {
  display: block;
}

.field-label {
  display: block;
  font-size: var(--font-size-body);
  color: var(--color-text-main);
  margin-bottom: 6px;
}

.required {
  color: var(--color-danger);
  font-style: normal;
}

.field-input,
.field-textarea {
  width: 100%;
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-sm);
  padding: 8px 12px;
  font-size: var(--font-size-body);
  font-family: inherit;
  outline: none;
  transition: var(--transition-base);
}

.field-input:focus,
.field-textarea:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(91, 107, 245, 0.25);
}

.field-textarea {
  resize: vertical;
  line-height: 1.6;
}

.field-textarea.mono {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
}

.field-error {
  display: block;
  margin-top: 4px;
  font-size: var(--font-size-small);
  color: var(--color-danger);
}

.submit-error {
  margin: 0;
  font-size: var(--font-size-body);
  color: var(--color-danger);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
}

.panel-wrap {
  position: sticky;
  top: var(--space-4);
}

@media (max-width: 1023px) {
  .create-layout {
    grid-template-columns: 1fr;
  }
  .panel-wrap {
    position: static;
  }
}
</style>
