// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：用户会话 Pinia store。承载 token 与用户信息持久化、登录/登出动作；
// 仅做会话管理，不做资源数据缓存（对照 AGENTS.md 4.5 状态管理策略）。

import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi } from '@/api/auth'
import type { LoginRequestDTO, UserDTO } from '@/types'

/** token 本地存储键；凭证不入日志与源码（对照 AGENTS.md 4.8） */
const TOKEN_KEY = 'harness-admin-token'
const USER_KEY = 'harness-admin-user'

/** 从本地存储恢复用户信息；JSON 损坏时按未登录处理 */
function readUser(): UserDTO | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as UserDTO
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) ?? '',
    user: readUser()
  }),
  getters: {
    /** 是否存在有效会话（是否有 token） */
    isAuthenticated: (state) => Boolean(state.token)
  },
  actions: {
    /**
     * 登录：调后端换取凭证并持久化会话，刷新页面后自动恢复
     * @param payload 登录请求体
     */
    async login(payload: LoginRequestDTO) {
      const result = await loginApi(payload)
      this.token = result.token
      this.user = result.user
      localStorage.setItem(TOKEN_KEY, result.token)
      localStorage.setItem(USER_KEY, JSON.stringify(result.user))
    },
    /**
     * 登出：调用后端吊销凭证并清理本地会话；
     * 后端幂等，吊销请求失败时仍保证本地会话被清理，不向调用方抛错
     */
    async logout() {
      try {
        await logoutApi()
      } catch {
        // 吊销失败不影响本地清理（对照 AGENTS.md 4.5：登出幂等兜底）
      }
      this.clearSession()
    },
    /** 清理本地会话（登出或 401 回调统一调用） */
    clearSession() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})
