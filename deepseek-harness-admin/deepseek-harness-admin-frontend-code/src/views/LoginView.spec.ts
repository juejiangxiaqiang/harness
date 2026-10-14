// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：登录页组件测试。覆盖空表单校验、登录成功跳转原地址、登录失败内联错误；
// 认证接口以 vi.mock 隔离，路由使用内存历史。

import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, type VueWrapper } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter, type Router } from 'vue-router'
import LoginView from './LoginView.vue'
import { ApiError } from '@/api/apiClient'

vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn()
}))

import { login as loginApi } from '@/api/auth'

const mockedLogin = vi.mocked(loginApi)

/** 等待异步队列排空：登录接口与路由导航均为异步操作 */
function flushPromises(): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, 0))
}

/** 挂载登录页：真实内存路由 + 独立 Pinia，避免测试间状态污染 */
async function mountLogin(): Promise<{ wrapper: VueWrapper; router: Router }> {
  setActivePinia(createPinia())
  localStorage.clear()
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/login', component: LoginView },
      { path: '/knowledge-bases', component: { template: '<div>知识库页</div>' } },
      { path: '/', redirect: '/knowledge-bases' }
    ]
  })
  await router.push('/login')
  await router.isReady()
  const wrapper = mount(LoginView, { global: { plugins: [router] } })
  return { wrapper, router }
}

describe('LoginView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('空表单提交给出内联提示', async () => {
    const { wrapper } = await mountLogin()
    await wrapper.find('form').trigger('submit')
    expect(wrapper.find('.inline-error').text()).toBe('请输入账号')
  })

  it('登录成功跳转到首页', async () => {
    mockedLogin.mockResolvedValue({ token: 'tk', user: { id: 1, username: 'admin', role: 'ADMIN' } })
    const { wrapper, router } = await mountLogin()
    await wrapper.find('input[type="text"]').setValue('admin')
    await wrapper.find('input[type="password"]').setValue('admin123')
    await wrapper.find('form').trigger('submit')
    // 等待登录与路由导航的异步完成
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/knowledge-bases')
  })

  it('登录失败展示映射后的内联错误', async () => {
    mockedLogin.mockRejectedValue(new ApiError('AUTH_INVALID_CREDENTIALS', '账号或密码错误'))
    const { wrapper, router } = await mountLogin()
    await wrapper.find('input[type="text"]').setValue('admin')
    await wrapper.find('input[type="password"]').setValue('wrong')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(wrapper.find('.inline-error').text()).toBe('账号或密码错误')
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('登录成功时记住账号写入本地存储', async () => {
    mockedLogin.mockResolvedValue({ token: 'tk', user: { id: 1, username: 'admin', role: 'ADMIN' } })
    const { wrapper } = await mountLogin()
    await wrapper.find('input[type="text"]').setValue('admin')
    await wrapper.find('input[type="password"]').setValue('admin123')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(localStorage.getItem('harness-admin-remember-username')).toBe('admin')
  })
})
