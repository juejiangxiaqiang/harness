// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：Skills 列表页组件测试。覆盖列表渲染、空态引导与创建入口跳转。

import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import SkillsView from './SkillsView.vue'

vi.mock('@/api/skill', () => ({
  fetchSkills: vi.fn()
}))

import { fetchSkills } from '@/api/skill'
import type { SkillDTO } from '@/types'

const mockedFetch = vi.mocked(fetchSkills)

const skill: SkillDTO = {
  id: 1,
  name: '制度问答',
  description: '制度检索与问答',
  content: '内容',
  status: 'ACTIVE',
  version: 'v1',
  knowledgeBaseIds: [1],
  createdAt: '2026-10-07T10:00:00'
}

/** 挂载：提供内存路由满足 useRouter 依赖，el-pagination/el-icon 以 stub 替代 */
async function mountView() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/skills', component: SkillsView },
      { path: '/skills/new', name: 'skill-create', component: { template: '<div>新建</div>' } }
    ]
  })
  await router.push('/skills')
  await router.isReady()
  const wrapper = mount(SkillsView, {
    global: {
      plugins: [router],
      stubs: {
        'el-icon': { template: '<span><slot /></span>' },
        'el-pagination': { template: '<div class="pager-stub" />' }
      }
    }
  })
  return { wrapper, router }
}

describe('SkillsView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('渲染 Skill 列表行', async () => {
    mockedFetch.mockResolvedValue({ page: 1, size: 10, total: 1, items: [skill] })
    const { wrapper } = await mountView()
    await flushPromises()
    expect(wrapper.get('.cell-name').text()).toBe('制度问答')
    expect(wrapper.text()).toContain('v1')
    expect(mockedFetch).toHaveBeenCalledWith({ page: 1, size: 10 })
  })

  it('空列表展示空态引导', async () => {
    mockedFetch.mockResolvedValue({ page: 1, size: 10, total: 0, items: [] })
    const { wrapper } = await mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('暂无 Skill')
  })

  it('点击空态引导跳转创建页', async () => {
    mockedFetch.mockResolvedValue({ page: 1, size: 10, total: 0, items: [] })
    const { wrapper, router } = await mountView()
    await flushPromises()
    await wrapper.find('.btn-pill.primary').trigger('click')
    // 等待路由导航异步完成
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/skills/new')
  })
})
