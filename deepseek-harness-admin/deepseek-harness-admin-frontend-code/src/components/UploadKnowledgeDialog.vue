<!--
---
last_updated: 2026-10-07
status: active          # active | deprecated | draft
owner: @qiang
---
文件职责：知识库上传弹窗。拖拽/点击选文件、类型与大小预校验、逐文件进度、
防重复提交；上传成功后提示"自动解析/切分/向量化"并通知父页面刷新列表
（校验规则对齐 docs/API_SPEC.md 第 3 章：txt/md/pdf/docx，≤20MB）。
-->
<script setup lang="ts">
// @ts-ignore - 解决 vue 模块声明文件未找到的类型检查问题
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadFile } from 'element-plus'
import { Document, Delete, UploadFilled } from '@element-plus/icons-vue'
import { uploadKnowledgeBase } from '@/api/knowledgeBase'
import { extractExtension, formatBytes } from '@/utils/format'

/** 后端允许的扩展名与单文件大小上限（对齐 API_SPEC.md 第 3 章） */
const ALLOWED_EXTENSIONS = ['txt', 'md', 'pdf', 'docx']
const MAX_FILE_SIZE = 20 * 1024 * 1024

/** 待上传文件条目：status 记录上传阶段，progress 为 0-100 */
interface UploadItem {
  file: File
  status: 'ready' | 'uploading' | 'done' | 'error'
  progress: number
  error?: string
}

defineProps<{
  /** 弹窗可见性（受控） */
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'uploaded'): void
}>()

const fileList = ref<UploadItem[]>([])
const uploading = ref(false)

/**
 * 前端预校验：类型与大小对齐后端限制，不通过则拒绝并给出原因
 * @param file 待校验文件
 * @returns 校验失败原因，通过返回 null
 */
function validateFile(file: File): string | null {
  const ext = extractExtension(file.name)
  if (!ALLOWED_EXTENSIONS.includes(ext)) {
    return `不支持 ${ext || '无扩展名'} 格式，仅支持 ${ALLOWED_EXTENSIONS.join('/')}`
  }
  if (file.size > MAX_FILE_SIZE) {
    return `文件超过 ${MAX_FILE_SIZE / 1024 / 1024}MB 上限`
  }
  return null
}

/** 选择文件（拖拽或点击），追加到待上传列表 */
function onFilesSelected(files: File[]) {
  for (const file of files) {
    const reason = validateFile(file)
    if (reason) {
      ElMessage.warning(`${file.name}：${reason}`)
      continue
    }
    fileList.value.push({ file, status: 'ready', progress: 0 })
  }
}

/** 移除某个待上传文件（上传中禁止移除） */
function removeItem(index: number) {
  if (fileList.value[index].status === 'uploading') return
  fileList.value.splice(index, 1)
}

/** 开始上传：逐文件并发上传并更新进度；全部结束后通知父页面刷新 */
async function startUpload() {
  if (uploading.value || fileList.value.length === 0) return
  uploading.value = true
  await Promise.all(
    fileList.value.map(async (item) => {
      item.status = 'uploading'
      try {
        await uploadKnowledgeBase(item.file, undefined, (percent) => {
          item.progress = percent
        })
        item.status = 'done'
      } catch (e) {
        item.status = 'error'
        item.error = e instanceof Error ? e.message : '上传失败'
      }
    })
  )
  uploading.value = false
  const failed = fileList.value.filter((it: UploadItem) => it.status === 'error').length
  if (failed === 0) {
    ElMessage.success('上传完成，系统将自动解析、切分并向量化')
    // 全部成功后关闭弹窗并触发列表刷新
    emit('uploaded')
    emit('update:visible', false)
    fileList.value = []
  } else {
    ElMessage.error(`${failed} 个文件上传失败，请查看列表原因后重试`)
  }
}

/** 关闭弹窗：上传中禁止关闭，避免任务失控 */
function onClose() {
  if (uploading.value) return
  emit('update:visible', false)
  fileList.value = []
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="上传知识库"
    width="560px"
    :close-on-click-modal="!uploading"
    @close="onClose"
  >
    <el-upload
      drag
      multiple
      :auto-upload="false"
      :show-file-list="false"
      accept=".txt,.md,.pdf,.docx"
      :disabled="uploading"
      @change="(f: UploadFile) => onFilesSelected(f.raw ? [f.raw] : [])"
    >
      <el-icon class="upload-icon" :size="40"><UploadFilled /></el-icon>
      <div class="el-upload__text">拖拽文件到此处，或 <em>点击选择文件</em></div>
      <div class="upload-hint">支持 TXT / MD / PDF / DOCX，单个文件 ≤ 20MB</div>
    </el-upload>

    <ul v-if="fileList.length" class="file-list">
      <li v-for="(item, idx) in fileList" :key="idx" class="file-item">
        <el-icon :size="16" class="file-icon"><Document /></el-icon>
        <span class="file-name">{{ item.file.name }}</span>
        <span class="file-size">{{ formatBytes(item.file.size) }}</span>
        <span v-if="item.status === 'uploading'" class="file-status">
          <el-progress :percentage="item.progress" :width="80" :show-text="false" />
        </span>
        <span v-else-if="item.status === 'done'" class="file-status ok">√ 已上传</span>
        <span v-else-if="item.status === 'error'" class="file-status err">{{ item.error }}</span>
        <span v-else class="file-status">待上传</span>
        <button
          v-if="item.status !== 'uploading'"
          class="file-remove"
          aria-label="移除文件"
          @click="removeItem(idx)"
        >
          <el-icon :size="14"><Delete /></el-icon>
        </button>
      </li>
    </ul>

    <p class="upload-note">上传完成后将自动开始解析、切分、向量化，处理完成后方可用于 Skill 关联。</p>

    <template #footer>
      <el-button :disabled="uploading" @click="onClose">取消</el-button>
      <el-button type="primary" :loading="uploading" :disabled="fileList.length === 0" @click="startUpload">
        {{ uploading ? '上传中…' : '开始上传' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.upload-icon {
  color: var(--color-primary);
}

.upload-hint {
  font-size: var(--font-size-small);
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.file-list {
  list-style: none;
  margin: var(--space-3) 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: 8px 10px;
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-sm);
}

.file-icon {
  color: var(--color-accent-blue);
}

.file-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--font-size-body);
}

.file-size {
  font-size: var(--font-size-small);
  color: var(--color-text-secondary);
}

.file-status {
  font-size: var(--font-size-small);
  color: var(--color-text-secondary);
}

.file-status.ok {
  color: var(--color-success);
}

.file-status.err {
  color: var(--color-danger);
}

.file-remove {
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  padding: 4px;
}

.file-remove:hover {
  color: var(--color-danger);
}

.upload-note {
  font-size: var(--font-size-small);
  color: var(--color-text-placeholder);
  margin: var(--space-2) 0 0;
}
</style>
