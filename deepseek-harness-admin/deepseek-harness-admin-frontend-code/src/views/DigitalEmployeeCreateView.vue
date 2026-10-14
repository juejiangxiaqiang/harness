<!--
---
last_updated: 2026-10-08
status: active          # active | deprecated | draft
owner: @qiang
---
文件职责：创建数字员工页。基础信息 + 系统提示词 + 模型配置 + Skill 多选 +
发布校验结果区；保存草稿与发布分离，发布失败禁用并红字列明原因
（对照计划书 P3、VISUAL_DESIGN.md 4.4 与 API_SPEC.md 第 5 章）。
-->
<script setup lang="ts">
// @ts-ignore - 解决 vue 模块声明文件未找到的类型检查问题
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchSkills } from '@/api/skill'
import { createDigitalEmployee, publishDigitalEmployee } from '@/api/digitalEmployee'
import { ApiError } from '@/api/apiClient'
import type { SkillDTO } from '@/types'
import CardSelectPanel, { type SelectItem } from '@/components/CardSelectPanel.vue'
import SkeletonTable from '@/components/SkeletonTable.vue'

const router = useRouter()

/** 表单：name 必填 ≤128；systemPrompt 必填 ≤20000；skillIds ≥1 */
const form = reactive({
  name: '',
  description: '',
  systemPrompt: '',
  model: 'deepseek-chat',
  temperature: 0.7,
  maxTokens: 2048
})
const fieldErrors = ref<Record<string, string>>({})
const submitError = ref('')
const submitting = ref(false)

/** Skill 候选与已选 ID */
const skillItems = ref<SelectItem[]>([])
const selectedIds = ref<number[]>([])
const skillLoading = ref(true)

/** 已创建的数字员工 ID：草稿成功后复用，发布无需重复创建 */
const createdId = ref<number | null>(null)

/** 头像预览渐变：仅本地视觉演示，不随创建/发布提交 */
const AVATAR_PRESETS = [
  'linear-gradient(135deg, var(--color-primary), var(--color-accent-blue))',
  'linear-gradient(135deg, var(--color-accent-purple), var(--color-primary))',
  'linear-gradient(135deg, var(--color-accent-teal), #3bae7d)',
  'linear-gradient(135deg, #f5a623, #f76b1c)'
]
const activeAvatar = ref(0)

/** 发布校验结果区：本地前置校验项 */
const validationChecks = computed(() => [
  { label: '名称、系统提示词与模型配置已填写', pass: validateRequired() },
  { label: '已选择至少一个 Skill', pass: selectedIds.value.length > 0 }
])

/** 本地必填校验（不含 Skill 选择） */
function validateRequired(): boolean {
  return Boolean(form.name.trim()) && Boolean(form.systemPrompt.trim()) && Boolean(form.model)
}

/** 字段级校验 */
function validateField(key: 'name' | 'description' | 'systemPrompt'): boolean {
  const errors: Record<string, string> = { ...fieldErrors.value }
  if (key === 'name') {
    if (!form.name.trim()) errors[key] = '名称必填'
    else if (form.name.length > 128) errors[key] = '名称不能超过 128 个字符'
    else delete errors[key]
  } else if (key === 'description') {
    if (form.description.length > 500) errors[key] = '描述不能超过 500 个字符'
    else delete errors[key]
  } else if (key === 'systemPrompt') {
    if (!form.systemPrompt.trim()) errors[key] = '系统提示词必填，作为数字员工行为底座'
    else if (form.systemPrompt.length > 20000) errors[key] = '提示词不能超过 20000 个字符'
    else delete errors[key]
  }
  fieldErrors.value = errors
  return !errors[key]
}

/**
 * 加载 Skill 候选：展示关联知识库数量，帮助判断可发布性
 */
async function loadSkills() {
  skillLoading.value = true
  try {
    const res = await fetchSkills({ page: 1, size: 100 })
    skillItems.value = res.items.map((s: SkillDTO) => ({
      id: s.id,
      label: s.name,
      subtitle: `关联知识库 ×${s.knowledgeBaseIds.length}`,
      icon: s.name.charAt(0)
    }))
  } finally {
    skillLoading.value = false
  }
}

/** 保存草稿：按后端必填校验创建，成功回列表 */
async function handleSaveDraft() {
  if (submitting.value) return
  submitError.value = ''
  const ok = [validateField('name'), validateField('systemPrompt')].every(Boolean)
  if (!ok) return
  if (selectedIds.value.length === 0) {
    submitError.value = '请至少选择一个 Skill'
    return
  }
  submitting.value = true
  try {
    const res = await createDigitalEmployee(buildRequest())
    createdId.value = res.id
    ElMessage.success('草稿已保存')
    router.push({ name: 'digital-employees' })
  } catch (e) {
    submitError.value = e instanceof ApiError ? e.message : '保存失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

/** 组装创建请求体：不携带 knowledgeBaseIds（由 skillIds 间接确定） */
function buildRequest() {
  return {
    name: form.name.trim(),
    description: form.description.trim(),
    systemPrompt: form.systemPrompt.trim(),
    skillIds: selectedIds.value,
    modelConfig: {
      model: form.model,
      temperature: form.temperature,
      maxTokens: form.maxTokens
    }
  }
}

/** 发布：本地校验 → 创建（如未创建）→ 发布；失败展示原因，成功跳预览页 */
async function handlePublish() {
  if (submitting.value) return
  submitError.value = ''
  if (!validationChecks.value.every((c) => c.pass)) {
    submitError.value = '存在未通过的校验项，请先修复'
    return
  }
  submitting.value = true
  try {
    // 尚未创建时先创建 DRAFT，再发布
    if (createdId.value === null) {
      const created = await createDigitalEmployee(buildRequest())
      createdId.value = created.id
    }
    const release = await publishDigitalEmployee(createdId.value!)
    if (release.status === 'SUCCESS') {
      ElMessage.success(`发布成功，版本 ${release.version}`)
      router.push({ name: 'employee-preview', params: { id: createdId.value } })
    } else {
      // 后端发布校验失败：红字列出失败原因
      submitError.value = release.failureReason || '发布未通过校验，请检查 Skill 及其知识库状态'
    }
  } catch (e) {
    submitError.value = e instanceof ApiError ? e.message : '发布失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadSkills()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">创建数字员工</h1>
        <p class="page-subtitle">配置行为底座、模型与关联 Skills，发布后供普通用户使用</p>
      </div>
    </div>

    <div class="create-stack">
      <!-- 基础信息 -->
      <section class="card section">
        <h2 class="section-title">基础信息</h2>
        <label class="field">
            <span class="field-label">名称 <em class="required">*</em></span>
            <input
              v-model="form.name"
              class="field-input"
              type="text"
              placeholder="数字员工名称"
              @blur="validateField('name')"
            />
            <span v-if="fieldErrors.name" class="field-error">{{ fieldErrors.name }}</span>
          </label>
          <div class="field avatar-field">
            <span class="field-label">头像</span>
            <div class="avatar-picker">
              <div class="avatar-preview" :style="{ background: AVATAR_PRESETS[activeAvatar] }">
                {{ form.name.charAt(0) || '员' }}
              </div>
              <div class="avatar-presets">
                <button
                  v-for="(grad, idx) in AVATAR_PRESETS"
                  :key="idx"
                  type="button"
                  class="avatar-preset"
                  :class="{ active: activeAvatar === idx }"
                  :style="{ background: grad }"
                  :aria-label="`头像预设 ${idx + 1}`"
                  @click="activeAvatar = idx"
                />
              </div>
            </div>
          </div>
        <p class="avatar-tip">提示：头像仅用于预览演示，当前版本不随数字员工保存或发布。</p>
        <label class="field">
          <span class="field-label">描述</span>
          <textarea
            v-model="form.description"
            class="field-textarea"
            rows="2"
            placeholder="职责说明（选填）"
            @blur="validateField('description')"
          />
          <span v-if="fieldErrors.description" class="field-error">{{ fieldErrors.description }}</span>
        </label>
      </section>

      <!-- 系统提示词 -->
      <section class="card section">
        <h2 class="section-title">系统提示词 <em class="required">*</em></h2>
        <textarea
          v-model="form.systemPrompt"
          class="field-textarea mono"
          rows="6"
          placeholder="作为数字员工行为底座，发布后不可修改"
          @blur="validateField('systemPrompt')"
        />
        <div class="char-count">{{ form.systemPrompt.length }}/20000</div>
        <span v-if="fieldErrors.systemPrompt" class="field-error">{{ fieldErrors.systemPrompt }}</span>
        <p class="section-tip">提示：系统提示词将作为数字员工的行为底座，发布后不可修改。</p>
      </section>

      <!-- 模型配置 -->
      <section class="card section">
        <h2 class="section-title">模型配置</h2>
        <div class="model-row">
          <label class="field">
            <span class="field-label">模型 <em class="required">*</em></span>
            <el-select v-model="form.model" class="model-select" allow-create filterable placeholder="选择或输入模型名">
              <el-option label="deepseek-chat" value="deepseek-chat" />
              <el-option label="qwen-plus" value="qwen-plus" />
              <el-option label="configured-model" value="configured-model" />
            </el-select>
          </label>
          <label class="field">
            <span class="field-label">温度 {{ form.temperature }}</span>
            <el-slider v-model="form.temperature" :min="0" :max="1" :step="0.1" class="temp-slider" />
          </label>
          <label class="field">
            <span class="field-label">上下文长度</span>
            <el-input-number v-model="form.maxTokens" :min="256" :max="8192" :step="256" />
          </label>
        </div>
      </section>

      <!-- 关联 Skills -->
      <SkeletonTable v-if="skillLoading" :columns="1" :rows="4" />
      <CardSelectPanel
        v-else
        v-model="selectedIds"
        title="关联 Skills"
        :items="skillItems"
      />
    </div>

    <!-- 发布校验结果区 -->
    <section class="card section validation-panel">
      <h2 class="section-title">发布校验</h2>
      <ul class="check-list">
        <li
          v-for="check in validationChecks"
          :key="check.label"
          class="check-item"
          :class="check.pass ? 'pass' : 'fail'"
        >
          {{ check.pass ? '✓' : '✗' }} {{ check.label }}
        </li>
      </ul>
      <p v-if="submitError" class="submit-error" role="alert">{{ submitError }}</p>
      <div class="form-actions">
        <button type="button" class="btn-pill" @click="router.push({ name: 'digital-employees' })">取消</button>
        <button type="button" class="btn-pill" :disabled="submitting" @click="handleSaveDraft">
          保存草稿
        </button>
        <button
          type="button"
          class="btn-pill primary"
          :disabled="submitting || !validationChecks.every((c) => c.pass)"
          @click="handlePublish"
        >
          {{ submitting ? '处理中…' : '发布数字员工' }}
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.create-stack {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  max-width: 1080px;
}

.section {
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.section-title {
  margin: 0;
  font-size: var(--font-size-card);
  font-weight: var(--font-weight-card);
}

.required {
  color: var(--color-danger);
  font-style: normal;
}

.field {
  display: block;
}

/* 头像选择器：大预览圆 + 预设渐变圆（对照 style.css .avatar-picker/.avatar-big） */
.avatar-picker {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-preview {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  font-weight: var(--font-weight-card);
  flex-shrink: 0;
}

.avatar-presets {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.avatar-preset {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid transparent;
  transition: var(--transition-base);
}

.avatar-preset:hover {
  transform: scale(1.1);
}

.avatar-preset.active {
  border-color: var(--color-text-main);
}

/* 头像仅演示提示：弱化小字 */
.avatar-tip {
  margin: 0;
  font-size: var(--font-size-small);
  color: var(--color-text-placeholder);
}

.field-label {
  display: block;
  font-size: var(--font-size-body);
  color: var(--color-text-main);
  margin-bottom: 6px;
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

.char-count {
  align-self: flex-end;
  font-size: var(--font-size-small);
  color: var(--color-text-placeholder);
}

.field-error {
  display: block;
  font-size: var(--font-size-small);
  color: var(--color-danger);
}

.section-tip {
  margin: 0;
  font-size: var(--font-size-small);
  color: var(--color-text-placeholder);
}

.model-row {
  display: grid;
  grid-template-columns: 1fr 1.4fr 1fr;
  gap: var(--space-3);
  align-items: start;
}

.temp-slider {
  margin-top: 26px;
}

.validation-panel {
  max-width: 1080px;
  margin-top: var(--space-3);
}

.check-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.check-item {
  font-size: var(--font-size-body);
  padding: 6px 10px;
  border-radius: var(--radius-sm);
}

.check-item.pass {
  color: var(--color-success);
  background: var(--color-success-bg);
}

.check-item.fail {
  color: var(--color-danger);
  background: var(--color-danger-bg);
}

.submit-error {
  margin: var(--space-2) 0 0;
  font-size: var(--font-size-body);
  color: var(--color-danger);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
}

@media (max-width: 767px) {
  .model-row {
    grid-template-columns: 1fr;
  }
}
</style>
