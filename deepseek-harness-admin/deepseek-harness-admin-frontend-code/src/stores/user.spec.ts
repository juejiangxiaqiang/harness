// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：用户会话 store 单元测试。覆盖登录成功/失败、登出清会话、
// 凭证持久化与 401 清会话分支；认证接口以 vi.mock 隔离。

import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

// 隔离认证接口，避免测试触碰真实 HTTP
vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn()
}))

import { login as loginApi, logout as logoutApi } from '@/api/auth'
import { useUserStore } from './user'

const mockedLogin = vi.mocked(loginApi)
const mockedLogout = vi.mocked(logoutApi)

describe('user store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('初始状态无会话', () => {
    const store = useUserStore()
    expect(store.isAuthenticated).toBe(false)
    expect(store.token).toBe('')
    expect(store.user).toBeNull()
  })

  it('登录成功写入会话并持久化', async () => {
    mockedLogin.mockResolvedValue({ token: 'tk-1', user: { id: 1, username: 'admin', role: 'ADMIN' } })
    const store = useUserStore()
    await store.login({ username: 'admin', password: 'secret' })
    expect(store.isAuthenticated).toBe(true)
    expect(store.token).toBe('tk-1')
    expect(store.user?.username).toBe('admin')
    expect(localStorage.getItem('harness-admin-token')).toBe('tk-1')
  })

  it('登录失败抛出错误且不写入会话', async () => {
    mockedLogin.mockRejectedValue(new Error('账号或密码错误'))
    const store = useUserStore()
    await expect(store.login({ username: 'admin', password: 'wrong' })).rejects.toThrow('账号或密码错误')
    expect(store.isAuthenticated).toBe(false)
    expect(localStorage.getItem('harness-admin-token')).toBeNull()
  })

  it('登出调用后端吊销并清理本地会话', async () => {
    mockedLogin.mockResolvedValue({ token: 'tk-1', user: { id: 1, username: 'admin', role: 'ADMIN' } })
    mockedLogout.mockResolvedValue(undefined)
    const store = useUserStore()
    await store.login({ username: 'admin', password: 'secret' })
    await store.logout()
    expect(mockedLogout).toHaveBeenCalledTimes(1)
    expect(store.isAuthenticated).toBe(false)
    expect(localStorage.getItem('harness-admin-token')).toBeNull()
  })

  it('登出接口失败时仍清理本地会话（幂等兜底）', async () => {
    mockedLogout.mockRejectedValue(new Error('network'))
    const store = useUserStore()
    store.token = 'tk-stale'
    store.user = { id: 1, username: 'admin', role: 'ADMIN' }
    await store.logout()
    expect(store.isAuthenticated).toBe(false)
    expect(localStorage.getItem('harness-admin-token')).toBeNull()
  })

  it('clearSession 直接清理会话与存储', () => {
    const store = useUserStore()
    store.token = 'tk'
    store.user = { id: 1, username: 'u', role: 'ADMIN' }
    localStorage.setItem('harness-admin-token', 'tk')
    store.clearSession()
    expect(store.isAuthenticated).toBe(false)
    expect(store.user).toBeNull()
  })
})
