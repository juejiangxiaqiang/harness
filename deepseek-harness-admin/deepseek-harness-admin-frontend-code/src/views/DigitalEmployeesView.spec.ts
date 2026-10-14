// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：数字员工列表页组件测试。覆盖卡片渲染、状态筛选与预览跳转。

import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import DigitalEmployeesView from './DigitalEmployeesView.vue'

vi.mock('@/api/digitalEmployee', () => ({
  fetchDigitalEmployees: vi.fn()
}))

import { fetchDigitalEmployees } from '@/api/digitalEmployee'
import type { DigitalEmployeeDTO } from '@/types'

const mockedFetch = vi.mocked(fetchDigitalEmployees)

const published: DigitalEmployeeDTO = {
  id: 1,
  name: '售后助手',
  description: '处理售后咨询',
  systemPrompt: 'p',
  modelConfig: {},
  status: 'PUBLISHED',
  publishedVersion: 'v1',
  skillIds: [1],
  createdAt: '2026-10-07T10:00:00'
}

const draft: DigitalEmployeeDTO = { ...published, id: 2, name: '草稿助手', status: 'DRAFT', publishedVersion: null }

async function mountView() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/digital-employees', component: DigitalEmployeesView },
      { path: '/digital-employees/:id/preview', name: 'employee-preview', component: { template: '<div>预览</div>' } }
    ]
  })
  await router.push('/digital-employees')
  await router.isReady()
  const wrapper = mount(DigitalEmployeesView, {
    global: {
      plugins: [router],
      stubs: { 'el-icon': { template: '<span><slot /></span>' } }
    }
  })
  return { wrapper, router }
}

describe('DigitalEmployeesView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('渲染卡片并展示发布状态', async () => {
    mockedFetch.mockResolvedValue({ page: 1, size: 100, total: 2, items: [published, draft] })
    const { wrapper } = await mountView()
    await flushPromises()
    expect(wrapper.findAll('.emp-card')).toHaveLength(2)
    expect(wrapper.text()).toContain('售后助手')
    expect(wrapper.text()).toContain('已发布')
  })

  it('状态筛选仅保留已发布卡片', async () => {
    mockedFetch.mockResolvedValue({ page: 1, size: 100, total: 2, items: [published, draft] })
    const { wrapper } = await mountView()
    await flushPromises()
    await wrapper.findAll('.capsule')[2].trigger('click') // 已发布
    expect(wrapper.findAll('.emp-card')).toHaveLength(1)
    expect(wrapper.text()).toContain('售后助手')
    expect(wrapper.text()).not.toContain('草稿助手')
  })

  it('点击卡片跳转预览页', async () => {
    mockedFetch.mockResolvedValue({ page: 1, size: 100, total: 1, items: [published] })
    const { wrapper, router } = await mountView()
    await flushPromises()
    await wrapper.find('.emp-card').trigger('click')
    // 等待路由导航异步完成
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/digital-employees/1/preview')
  })
})
