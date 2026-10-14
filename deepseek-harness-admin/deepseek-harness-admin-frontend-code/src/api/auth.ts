// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：认证相关接口封装（登录、登出），路径与 docs/API_SPEC.md 第 2 章对齐。

import { request } from './apiClient'
import type { LoginRequestDTO, LoginResultDTO } from '@/types'

/**
 * 账号密码登录，成功返回 Bearer 凭证与用户信息
 * @param payload 登录请求体（username/password）
 * @returns 登录结果（token + user）；失败抛 ApiError
 */
export function login(payload: LoginRequestDTO): Promise<LoginResultDTO> {
  return request({ url: '/auth/login', method: 'POST', data: payload })
}

/**
 * 退出登录：吊销当前凭证；后端幂等，重复退出不报错
 */
export function logout(): Promise<void> {
  return request({ url: '/auth/logout', method: 'POST' })
}
