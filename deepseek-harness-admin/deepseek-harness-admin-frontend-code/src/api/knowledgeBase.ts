// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：知识库接口封装（列表、详情、上传、重新向量化），
// 路径与请求方式对照 docs/API_SPEC.md 第 3 章。

import { request } from './apiClient'
import type { KnowledgeBaseDTO, PagedResult } from '@/types'

/** 列表查询参数：page 从 1 开始，size 单页上限 100，name 模糊匹配 */
export interface KnowledgeBaseQuery {
  page: number
  size: number
  name?: string
}

/**
 * 分页查询知识库列表
 * @param params 分页与名称筛选参数
 * @returns 分页结果（含 total 与 items）
 */
export function fetchKnowledgeBases(params: KnowledgeBaseQuery): Promise<PagedResult<KnowledgeBaseDTO>> {
  return request({ url: '/knowledge-bases', method: 'GET', params })
}

/**
 * 查询知识库详情（用于轮询向量化状态）
 * @param id 知识库 ID
 * @returns 知识库详情，含最新 status/failureReason
 */
export function fetchKnowledgeBase(id: number): Promise<KnowledgeBaseDTO> {
  return request({ url: `/knowledge-bases/${id}`, method: 'GET' })
}

/**
 * 上传知识库（multipart/form-data）：上传成功后自动触发异步向量化
 * @param file 待上传文件（txt/md/pdf/docx）
 * @param name 可选自定义名称
 * @param onProgress 上传进度回调（0-100）
 * @returns 上传后的知识库对象（含初始状态）
 */
export function uploadKnowledgeBase(
  file: File,
  name?: string,
  onProgress?: (percent: number) => void
): Promise<KnowledgeBaseDTO> {
  const form = new FormData()
  form.append('file', file)
  if (name) {
    form.append('name', name)
  }
  return request({
    url: '/knowledge-bases',
    method: 'POST',
    data: form,
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (e) => {
      if (onProgress && e.total) {
        onProgress(Math.round((e.loaded / e.total) * 100))
      }
    }
  })
}

/**
 * 重新向量化：清理旧向量后重新处理，仅 READY/FAILED 状态可调用
 * @param id 知识库 ID
 * @returns 重新进入处理流程后的知识库对象
 */
export function reprocessKnowledgeBase(id: number): Promise<KnowledgeBaseDTO> {
  return request({ url: `/knowledge-bases/${id}/reprocess`, method: 'POST' })
}
