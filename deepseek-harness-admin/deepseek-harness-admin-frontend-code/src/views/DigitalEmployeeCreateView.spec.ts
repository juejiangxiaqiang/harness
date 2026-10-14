// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：创建数字员工页组件测试。覆盖 Skill 勾选、发布前置校验、
// 创建 + 发布成功跳转预览；el-select/el-slider/el-input-number 以 v-model stub 替代。

import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { ElIcon, ElInput, ElTooltip } from 'element-plus'
import DigitalEmployeeCreateView from './DigitalEmployeeCreateView.vue'

vi.mock('@/api/skill', () => ({
  fetchSkills: vi.fn()
}))

vi.mock('@/api/digitalEmployee', () => ({
  createDigitalEmployee: vi.fn(),
  publishDigitalEmployee: vi.fn()
}))

import { fetchSkills } from '@/api/skill'
import { createDigitalEmployee, publishDigitalEmployee } from '@/api/digitalEmployee'
import type { DigitalEmployeeDTO, ReleaseDTO, SkillDTO } from '@/types'

const mockedFetchSkills = vi.mocked(fetchSkills)
const mockedCreate = vi.mocked(createDigitalEmployee)
const mockedPublish = vi.mocked(publishDigitalEmployee)

const skill: SkillDTO = {
  id: 201,
  name: '制度问答',
  description: '制度检索与问答',
  content: '内容',
  status: 'ACTIVE',
  version: 'v1',
  knowledgeBaseIds: [1],
  createdAt: '2026-10-07T10:00:00'
}

/** 复杂 el-* 控件的 v-model stub，保证表单绑定可用 */
const elStubs = {
  'el-select': {
    props: ['modelValue'],
    emits: ['update:modelValue'],
    template:
      '<select :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><slot /></select>'
  },
  'el-slider': {
    props: ['modelValue'],
    emits: ['update:modelValue'],
    template:
      '<input type="range" :value="modelValue" @change="$emit(\'update:modelValue\', Number($event.target.value))" />'
  },
  'el-input-number': {
    props: ['modelValue'],
    emits: ['update:modelValue'],
    template:
      '<input type="number" :value="modelValue" @change="$emit(\'update:modelValue\', Number($event.target.value))" />'
  }
}

async function mountView() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/digital-employees/new', component: DigitalEmployeeCreateView },
      { path: '/digital-employees', name: 'digital-employees', component: { template: '<div>列表</div>' } },
      { path: '/digital-employees/:id/preview', name: 'employee-preview', component: { template: '<div>预览</div>' } }
    ]
  })
  await router.push('/digital-employees/new')
  await router.isReady()
  const wrapper = mount(DigitalEmployeeCreateView, {
    global: {
      plugins: [router],
      components: { ElInput, ElTooltip, ElIcon },
      stubs: elStubs
    }
  })
  return { wrapper, router }
}

describe('DigitalEmployeeCreateView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('未选 Skill 时发布按钮禁用并展示失败校验项', async () => {
    mockedFetchSkills.mockResolvedValue({ page: 1, size: 100, total: 1, items: [skill] })
    const { wrapper } = await mountView()
    await flushPromises()
    await wrapper.get('input[placeholder="数字员工名称"]').setValue('售后助手')
    await wrapper.get('textarea[placeholder="作为数字员工行为底座，发布后不可修改"]').setValue('你是助手。')
    expect(wrapper.text()).toContain('✗ 已选择至少一个 Skill')
    const publishBtn = wrapper.findAll('button.btn-pill.primary')
    expect(publishBtn[0].attributes('disabled')).toBeDefined()
  })

  it('勾选 Skill 后创建并发布成功跳转预览', async () => {
    mockedFetchSkills.mockResolvedValue({ page: 1, size: 100, total: 1, items: [skill] })
    mockedCreate.mockResolvedValue({
      id: 1,
      name: '售后助手',
      description: '',
      systemPrompt: '你是助手。',
      modelConfig: {},
      status: 'DRAFT',
      publishedVersion: null,
      skillIds: [201],
      createdAt: '2026-10-07T10:00:00'
    } as DigitalEmployeeDTO)
    mockedPublish.mockResolvedValue({
      id: 10,
      digitalEmployeeId: 1,
      version: 'v1',
      status: 'SUCCESS',
      failureReason: null,
      releasedAt: '2026-10-07T10:00:00'
    } as ReleaseDTO)
    const { wrapper, router } = await mountView()
    await flushPromises()
    await wrapper.get('input[placeholder="数字员工名称"]').setValue('售后助手')
    await wrapper.get('textarea[placeholder="作为数字员工行为底座，发布后不可修改"]').setValue('你是助手。')
    await wrapper.find('.panel-item').trigger('click')
    await wrapper.findAll('button.btn-pill.primary')[0].trigger('click')
    await flushPromises()
    // 创建请求不携带 knowledgeBaseIds，发布使用创建返回的 ID
    expect(mockedCreate).toHaveBeenCalledWith(
      expect.objectContaining({
        name: '售后助手',
        systemPrompt: '你是助手。',
        skillIds: [201],
        modelConfig: expect.objectContaining({ model: 'deepseek-chat' })
      })
    )
    expect(mockedPublish).toHaveBeenCalledWith(1)
    expect(router.currentRoute.value.path).toBe('/digital-employees/1/preview')
  })

  it('发布失败展示后端失败原因', async () => {
    mockedFetchSkills.mockResolvedValue({ page: 1, size: 100, total: 1, items: [skill] })
    mockedCreate.mockResolvedValue({
      id: 1,
      name: '售后助手',
      description: '',
      systemPrompt: '你是助手。',
      modelConfig: {},
      status: 'DRAFT',
      publishedVersion: null,
      skillIds: [201],
      createdAt: '2026-10-07T10:00:00'
    } as DigitalEmployeeDTO)
    mockedPublish.mockResolvedValue({
      id: 11,
      digitalEmployeeId: 1,
      version: 'v1',
      status: 'FAILED',
      failureReason: '所选 Skill 关联知识库未完成向量化',
      releasedAt: '2026-10-07T10:00:00'
    } as ReleaseDTO)
    const { wrapper } = await mountView()
    await flushPromises()
    await wrapper.get('input[placeholder="数字员工名称"]').setValue('售后助手')
    await wrapper.get('textarea[placeholder="作为数字员工行为底座，发布后不可修改"]').setValue('你是助手。')
    await wrapper.find('.panel-item').trigger('click')
    await wrapper.findAll('button.btn-pill.primary')[0].trigger('click')
    await flushPromises()
    expect(wrapper.find('.submit-error').text()).toContain('所选 Skill 关联知识库未完成向量化')
  })
})
