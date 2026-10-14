// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：App 外壳布局组件测试。覆盖面包屑渲染与窄屏抽屉触发结构。

import { beforeEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import AppShell from './AppShell.vue'

/** 挂载外壳：业务路由以 stub 替代，仅验证壳结构与面包屑 */
async function mountShell() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      {
        path: '/knowledge-bases',
        component: AppShell,
        meta: { title: '知识库', breadcrumb: ['Harness 管理后台', '资源管理', '知识库'] }
      }
    ]
  })
  await router.push('/knowledge-bases')
  await router.isReady()
  const wrapper = mount(AppShell, {
    global: {
      plugins: [router],
      stubs: {
        SidebarMenu: { template: '<aside class="sidebar-stub" />' },
        'el-drawer': { template: '<div class="drawer-stub" />' },
        'el-icon': { template: '<span><slot /></span>' }
      }
    }
  })
  return wrapper
}

describe('AppShell', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('渲染面包屑：末级高亮', async () => {
    const wrapper = await mountShell()
    expect(wrapper.text()).toContain('Harness 管理后台')
    expect(wrapper.text()).toContain('资源管理')
    expect(wrapper.find('.crumb-current').text()).toBe('知识库')
  })

  it('存在窄屏抽屉触发器', async () => {
    const wrapper = await mountShell()
    expect(wrapper.find('.menu-trigger').exists()).toBe(true)
  })
})
