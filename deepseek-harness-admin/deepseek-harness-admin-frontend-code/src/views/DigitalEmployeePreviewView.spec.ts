// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：数字员工预览页组件测试。覆盖信息面板渲染、页签切换与发布新版本流程。

import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type DOMWrapper } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import DigitalEmployeePreviewView from './DigitalEmployeePreviewView.vue'

vi.mock('@/api/digitalEmployee', () => ({
  fetchDigitalEmployee: vi.fn(),
  publishDigitalEmployee: vi.fn()
}))

vi.mock('@/api/skill', () => ({
  fetchSkills: vi.fn()
}))

import { fetchDigitalEmployee, publishDigitalEmployee } from '@/api/digitalEmployee'
import { fetchSkills } from '@/api/skill'
import type { DigitalEmployeeDTO, ReleaseDTO, SkillDTO } from '@/types'

const mockedFetchEmployee = vi.mocked(fetchDigitalEmployee)
const mockedFetchSkills = vi.mocked(fetchSkills)
const mockedPublish = vi.mocked(publishDigitalEmployee)

const employee: DigitalEmployeeDTO = {
  id: 1,
  name: '售后助手',
  description: '处理售后咨询',
  systemPrompt: '你是售后服务助手。',
  modelConfig: { model: 'deepseek-chat', temperature: 0.7, maxTokens: 2048 },
  status: 'PUBLISHED',
  publishedVersion: 'v1',
  skillIds: [201],
  createdAt: '2026-10-07T10:00:00'
}

const skill: SkillDTO = {
  id: 201,
  name: '制度问答',
  description: 'd',
  content: 'c',
  status: 'ACTIVE',
  version: 'v1',
  knowledgeBaseIds: [1],
  createdAt: '2026-10-07T10:00:00'
}

async function mountView() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/digital-employees/:id/preview', component: DigitalEmployeePreviewView },
      { path: '/digital-employees', name: 'digital-employees', component: { template: '<div>列表</div>' } }
    ]
  })
  await router.push('/digital-employees/1/preview')
  await router.isReady()
  const wrapper = mount(DigitalEmployeePreviewView, {
    global: {
      plugins: [router],
      stubs: { 'el-icon': { template: '<span><slot /></span>' } }
    }
  })
  return { wrapper, router }
}

describe('DigitalEmployeePreviewView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('渲染信息面板与当前版本', async () => {
    mockedFetchEmployee.mockResolvedValue(employee)
    mockedFetchSkills.mockResolvedValue({ page: 1, size: 100, total: 1, items: [skill] })
    const { wrapper } = await mountView()
    await flushPromises()
    expect(wrapper.find('.info-name').text()).toBe('售后助手')
    expect(wrapper.find('.info-status').text()).toContain('已发布')
    expect(wrapper.find('.info-status').text()).toContain('v1')
    expect(wrapper.find('.skill-chip').text()).toContain('制度问答')
  })

  it('切换页签展示版本历史与配置', async () => {
    mockedFetchEmployee.mockResolvedValue(employee)
    mockedFetchSkills.mockResolvedValue({ page: 1, size: 100, total: 1, items: [skill] })
    const { wrapper } = await mountView()
    await flushPromises()
    await wrapper.findAll('.tab')[1].trigger('click') // 版本历史
    const historyBody = wrapper.findAll('.tab-body').find((b: DOMWrapper<Element>) => b.text().includes('后端暂未提供'))
    expect(historyBody).toBeTruthy()
    expect((historyBody!.element as HTMLElement).style.display).not.toBe('none')
    await wrapper.findAll('.tab')[2].trigger('click') // 配置
    expect(wrapper.find('.prompt-box').text()).toBe('你是售后服务助手。')
  })

  it('发布新版本成功更新版本号', async () => {
    mockedFetchEmployee.mockResolvedValue(employee)
    mockedFetchSkills.mockResolvedValue({ page: 1, size: 100, total: 1, items: [skill] })
    mockedPublish.mockResolvedValue({
      id: 20,
      digitalEmployeeId: 1,
      version: 'v2',
      status: 'SUCCESS',
      failureReason: null,
      releasedAt: '2026-10-07T10:00:00'
    } as ReleaseDTO)
    const { wrapper } = await mountView()
    await flushPromises()
    // 二次确认通过
    const { ElMessageBox } = await import('element-plus')
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm' as never)
    await wrapper.find('.page-actions .btn-pill.primary').trigger('click')
    await flushPromises()
    expect(mockedPublish).toHaveBeenCalledWith(1)
    // 发布后重新加载详情，展示新版本
    expect(wrapper.find('.info-status').text()).toContain('v1')
  })
})
