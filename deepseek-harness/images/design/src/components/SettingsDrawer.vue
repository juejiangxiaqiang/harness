<!-- 文件路径: design/src/components/SettingsDrawer.vue -->
<!-- 作用: 对话设置抽屉 —— 模型/温度/系统提示/回车发送，修改即自动保存 -->
<template>
  <el-drawer
    :model-value="modelValue"
    title="对话设置"
    :size="320"
    :append-to-body="true"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="settings-form">
      <!-- 模型选择 -->
      <div class="form-item">
        <label class="form-label">模型</label>
        <el-select v-model="settings.model" class="form-control" @change="apply">
          <el-option v-for="m in modelOptions" :key="m.value" :label="m.label" :value="m.value" />
        </el-select>
      </div>

      <!-- 温度调节 -->
      <div class="form-item">
        <label class="form-label">温度（回复随机性）</label>
        <el-slider v-model="settings.temperature" :min="0" :max="2" :step="0.1" @change="apply" />
        <span class="form-hint">当前值：{{ settings.temperature.toFixed(1) }}，越高回复越发散</span>
      </div>

      <!-- 系统提示词 -->
      <div class="form-item">
        <label class="form-label">系统提示词</label>
        <el-input
          v-model="settings.systemPrompt"
          type="textarea"
          :rows="4"
          placeholder="可选，用于设定助手行为"
          @blur="apply"
        />
      </div>

      <!-- 回车发送开关 -->
      <div class="form-item">
        <label class="form-label">回车发送</label>
        <el-switch v-model="settings.enterToSend" @change="apply" />
        <span class="form-hint">关闭后使用 Ctrl+Enter 发送</span>
      </div>

      <!-- 保存反馈 -->
      <div class="settings-tip">设置修改后自动保存，立即生效</div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
// 对话设置抽屉
import { ElMessage } from 'element-plus'
import type { ChatSettings } from '@/types'

// 设置内容（双向绑定）
const props = defineProps<{ modelValue: boolean; settings: ChatSettings }>()

// 向外广播：可见性与设置变更
const emit = defineEmits<{
  (e: 'update:modelValue', visible: boolean): void
  (e: 'update:settings', patch: Partial<ChatSettings>): void
}>()

// 可选模型列表（本地演示选项）
const modelOptions = [
  { value: 'deepseek-v4', label: 'DeepSeek V4（推荐）' },
  { value: 'deepseek-v3', label: 'DeepSeek V3' },
  { value: 'deepseek-r1', label: 'DeepSeek R1（推理）' }
]

// 设置变更回调：广播 patch 并提示
function apply(): void {
  emit('update:settings', {
    model: props.settings.model,
    temperature: props.settings.temperature,
    systemPrompt: props.settings.systemPrompt,
    enterToSend: props.settings.enterToSend
  })
  ElMessage.success('设置已保存')
}
</script>

<style scoped>
/* 设置表单 */
.settings-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding-top: 4px;
}

/* 表单项 */
.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 标签 */
.form-label {
  font-size: var(--font-size-body);
  font-weight: var(--font-weight-medium);
  color: var(--color-text);
}

/* 控件通栏 */
.form-control {
  width: 100%;
}

/* 辅助提示 */
.form-hint {
  font-size: var(--font-size-caption);
  color: var(--color-text-muted);
}

/* 底部说明 */
.settings-tip {
  padding: 10px 12px;
  background: var(--color-primary-lighter);
  color: var(--color-primary);
  font-size: var(--font-size-caption);
  border-radius: var(--radius-md);
  text-align: center;
}
</style>
