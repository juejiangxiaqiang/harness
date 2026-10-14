// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：侧边栏组件测试。覆盖导航项渲染、选中态与退出登录流程。

import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import SidebarMenu from './SidebarMenu.vue'

vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn()
}))

import { useUserStore } from '@/stores/user'

async function mountMenu() {
  setActivePinia(createPinia())
  localStorage.clear()
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/knowledge-bases', name: 'knowledge-bases', component: { template: '<div>KB</div>' } },
      { path: '/skills', name: 'skills', component: { template: '<div>S</div>' } },
      { path: '/digital-employees', name: 'digital-employees', component: { template: '<div>DE</div>' } },
      { path: '/login', name: 'login', component: { template: '<div>L</div>' } }
    ]
  })
  await router.push('/knowledge-bases')
  await router.isReady()
  const wrapper = mount(SidebarMenu, {
    global: { plugins: [router], stubs: { 'el-icon': { template: '<span><slot /></span>' } } }
  })
  return { wrapper, router }
}

describe('SidebarMenu', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('渲染三个导航项与当前选中态', async () => {
    const { wrapper } = await mountMenu()
    const links = wrapper.findAll('.menu-item')
    expect(links).toHaveLength(3)
    expect(links[0].classes()).toContain('router-link-active')
    expect(links[0].text()).toContain('知识库')
    expect(links[1].text()).toContain('Skills')
    expect(links[2].text()).toContain('数字员工')
  })

  it('展示当前用户首字头像', async () => {
    const { wrapper } = await mountMenu()
    const store = useUserStore()
    store.user = { id: 1, username: 'admin', role: 'ADMIN' }
    // 等待响应式 computed 重渲染
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.avatar').text()).toBe('A')
    expect(wrapper.find('.user-name').text()).toBe('admin')
  })

  it('退出登录后跳转登录页', async () => {
    const { wrapper, router } = await mountMenu()
    const store = useUserStore()
    store.token = 'tk'
    // 二次确认通过 + 登出接口成功
    const { ElMessageBox } = await import('element-plus')
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm' as never)
    await wrapper.find('.logout-btn').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/login')
    expect(store.isAuthenticated).toBe(false)
  })
})
