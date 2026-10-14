// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：创建 Skill 页组件测试。覆盖 READY 知识库勾选、表单校验与提交创建。

import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type DOMWrapper } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { ElIcon, ElInput, ElTooltip } from 'element-plus'
import SkillCreateView from './SkillCreateView.vue'

vi.mock('@/api/knowledgeBase', () => ({
  fetchKnowledgeBases: vi.fn()
}))

vi.mock('@/api/skill', () => ({
  createSkill: vi.fn()
}))

import { fetchKnowledgeBases } from '@/api/knowledgeBase'
import { createSkill } from '@/api/skill'
import type { KnowledgeBaseDTO, SkillDTO } from '@/types'

const mockedFetchKB = vi.mocked(fetchKnowledgeBases)
const mockedCreateSkill = vi.mocked(createSkill)

const kbReady: KnowledgeBaseDTO = {
  id: 1,
  name: '产品手册 v2',
  status: 'READY',
  fileName: 'product.md',
  chunkCount: 8,
  vectorStore: 'redis',
  embeddingModel: 'qwen',
  failureReason: null,
  createdAt: '2026-10-07T10:00:00'
}

const kbFailed: KnowledgeBaseDTO = { ...kbReady, id: 2, name: '合规制度库', status: 'FAILED', chunkCount: 0 }

/** 创建成功后返回的 Skill 对象 */
const createdSkill: SkillDTO = {
  id: 1,
  name: '制度问答',
  description: '',
  content: '内容',
  status: 'ACTIVE',
  version: 'v1',
  knowledgeBaseIds: [1],
  createdAt: '2026-10-07T10:00:00'
}

async function mountView() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/skills/new', component: SkillCreateView },
      { path: '/skills', name: 'skills', component: { template: '<div>列表</div>' } }
    ]
  })
  await router.push('/skills/new')
  await router.isReady()
  const wrapper = mount(SkillCreateView, {
    global: {
      plugins: [router],
      components: { ElInput, ElTooltip, ElIcon }
    }
  })
  return { wrapper, router }
}

describe('SkillCreateView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('加载知识库候选：FAILED 禁用、READY 可勾选', async () => {
    mockedFetchKB.mockResolvedValue({ page: 1, size: 100, total: 2, items: [kbReady, kbFailed] })
    const { wrapper } = await mountView()
    await flushPromises()
    const rows = wrapper.findAll('.panel-item')
    expect(rows).toHaveLength(2)
    const failedRow = rows.find((r: DOMWrapper<Element>) => r.classes().includes('disabled'))
    expect(failedRow).toBeTruthy()
    expect(wrapper.text()).toContain('产品手册 v2')
  })

  it('勾选 READY 知识库并提交创建', async () => {
    mockedFetchKB.mockResolvedValue({ page: 1, size: 100, total: 1, items: [kbReady] })
    mockedCreateSkill.mockResolvedValue(createdSkill)
    const { wrapper } = await mountView()
    await flushPromises()
    await wrapper.find('.panel-item').trigger('click')
    await wrapper.get('input[placeholder="Skill 名称"]').setValue('制度问答')
    await wrapper.get('textarea[placeholder="提示词模板 / 内容配置"]').setValue('你是制度专家。')
    await wrapper.findAll('button.btn-pill')[1].trigger('click') // 保存
    await flushPromises()
    expect(mockedCreateSkill).toHaveBeenCalledWith({
      name: '制度问答',
      description: '',
      content: '你是制度专家。',
      knowledgeBaseIds: [1]
    })
  })

  it('名称缺失时提交给出内联校验错误', async () => {
    mockedFetchKB.mockResolvedValue({ page: 1, size: 100, total: 1, items: [kbReady] })
    const { wrapper } = await mountView()
    await flushPromises()
    await wrapper.find('.panel-item').trigger('click')
    await wrapper.findAll('button.btn-pill')[1].trigger('click')
    expect(wrapper.text()).toContain('名称必填')
  })
})
