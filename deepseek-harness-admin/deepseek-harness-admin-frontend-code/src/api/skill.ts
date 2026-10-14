// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：Skill 接口封装（列表、详情、创建），路径对照 docs/API_SPEC.md 第 4 章。

import { request } from './apiClient'
import type { PagedResult, SkillDTO, SkillRequestDTO } from '@/types'

/** 列表查询参数：page 从 1 开始，size 单页上限 100 */
export interface SkillQuery {
  page: number
  size: number
}

/**
 * 分页查询 Skills 列表
 * @param params 分页参数
 * @returns 分页结果
 */
export function fetchSkills(params: SkillQuery): Promise<PagedResult<SkillDTO>> {
  return request({ url: '/skills', method: 'GET', params })
}

/**
 * 查询 Skill 详情
 * @param id Skill ID
 * @returns Skill 详情（含关联知识库 ID 列表）
 */
export function fetchSkill(id: number): Promise<SkillDTO> {
  return request({ url: `/skills/${id}`, method: 'GET' })
}

/**
 * 创建 Skill：请求体需包含 1 个及以上 READY 状态知识库，
 * 否则后端返回 KNOWLEDGE_BASE_NOT_READY
 * @param payload 创建请求体
 * @returns 新建的 Skill 对象
 */
export function createSkill(payload: SkillRequestDTO): Promise<SkillDTO> {
  return request({ url: '/skills', method: 'POST', data: payload })
}
