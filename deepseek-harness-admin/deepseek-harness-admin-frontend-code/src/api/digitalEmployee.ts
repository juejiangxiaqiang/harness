// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：数字员工接口封装（列表、详情、创建、发布、回滚、发布记录），
// 路径对照 docs/API_SPEC.md 第 5 章。

import { request } from './apiClient'
import type { DigitalEmployeeDTO, DigitalEmployeeRequestDTO, PagedResult, ReleaseDTO, RollbackRequestDTO } from '@/types'

/** 列表查询参数：page 从 1 开始，size 单页上限 100 */
export interface DigitalEmployeeQuery {
  page: number
  size: number
}

/**
 * 分页查询数字员工列表
 * @param params 分页参数
 * @returns 分页结果
 */
export function fetchDigitalEmployees(params: DigitalEmployeeQuery): Promise<PagedResult<DigitalEmployeeDTO>> {
  return request({ url: '/digital-employees', method: 'GET', params })
}

/**
 * 查询数字员工详情（含系统提示词与版本状态）
 * @param id 数字员工 ID
 * @returns 详情对象
 */
export function fetchDigitalEmployee(id: number): Promise<DigitalEmployeeDTO> {
  return request({ url: `/digital-employees/${id}`, method: 'GET' })
}

/**
 * 创建数字员工：请求体不接收 knowledgeBaseIds（多余字段被反序列化忽略），
 * 知识库由 skillIds 间接确定
 * @param payload 创建请求体
 * @returns 新建的数字员工对象（默认 DRAFT）
 */
export function createDigitalEmployee(payload: DigitalEmployeeRequestDTO): Promise<DigitalEmployeeDTO> {
  return request({ url: '/digital-employees', method: 'POST', data: payload })
}

/**
 * 发布数字员工：服务端校验名称/提示词/模型/Skills 及展开知识库状态，
 * 通过后生成不可变配置快照与递增版本号；失败返回失败原因发布记录
 * @param id 数字员工 ID
 * @returns 本次发布记录（SUCCESS 或 FAILED）
 */
export function publishDigitalEmployee(id: number): Promise<ReleaseDTO> {
  return request({ url: `/digital-employees/${id}/publish`, method: 'POST' })
}

/**
 * 回滚到指定发布记录：只能选本数字员工且 SUCCESS 的记录，
 * 回滚不修改历史快照，而是基于所选快照生成新的生效记录
 * @param id 数字员工 ID
 * @param releaseId 目标发布记录 ID
 * @returns 新的生效发布记录
 */
export function rollbackDigitalEmployee(id: number, releaseId: number): Promise<ReleaseDTO> {
  const payload: RollbackRequestDTO = { releaseId }
  return request({ url: `/digital-employees/${id}/rollback`, method: 'POST', data: payload })
}
