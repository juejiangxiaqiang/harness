// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：知识库列表页组件测试。覆盖列表渲染、状态筛选与重新向量化触发；
// 上传弹窗以 stub 隔离，聚焦列表逻辑。

import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import KnowledgeBasesView from './KnowledgeBasesView.vue'

vi.mock('@/api/knowledgeBase', () => ({
  fetchKnowledgeBases: vi.fn(),
  reprocessKnowledgeBase: vi.fn()
}))

import { fetchKnowledgeBases, reprocessKnowledgeBase } from '@/api/knowledgeBase'
import type { KnowledgeBaseDTO } from '@/types'

const mockedFetch = vi.mocked(fetchKnowledgeBases)
const mockedReprocess = vi.mocked(reprocessKnowledgeBase)

const ready: KnowledgeBaseDTO = {
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

const failed: KnowledgeBaseDTO = { ...ready, id: 2, name: '合规制度库', status: 'FAILED', fileName: 'policy.pdf', chunkCount: 0 }

/** 上传弹窗 stub：隐藏 el-dialog 依赖，保留上传成功回调 */
const UploadStub = {
  name: 'UploadKnowledgeDialog',
  props: ['visible'],
  emits: ['update:visible', 'uploaded'],
  template: '<div class="upload-stub" />'
}

async function mountView() {
  const wrapper = mount(KnowledgeBasesView, {
    global: {
      stubs: {
        UploadKnowledgeDialog: UploadStub,
        'el-input': {
          props: ['modelValue'],
          emits: ['update:modelValue'],
          template:
            '<input class="el-input-stub" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
        },
        'el-pagination': { template: '<div class="pager-stub" />' },
        'el-icon': { template: '<span><slot /></span>' }
      }
    }
  })
  return wrapper
}

describe('KnowledgeBasesView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // 页面挂载时启动 4s 轮询定时器：以假定时器防止测试间泄漏与真实轮询
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('渲染知识库行与状态标签', async () => {
    mockedFetch.mockResolvedValue({ page: 1, size: 100, total: 2, items: [ready, failed] })
    const wrapper = await mountView()
    await flushPromises()
    expect(wrapper.findAll('tbody tr')).toHaveLength(2)
    expect(wrapper.text()).toContain('产品手册 v2')
    expect(wrapper.findAll('.status-tag')).toHaveLength(2)
    // READY/FAILED 行均可重新向量化
    expect(wrapper.findAll('.link-btn')).toHaveLength(2)
  })

  it('状态筛选仅展示失败条目', async () => {
    mockedFetch.mockResolvedValue({ page: 1, size: 100, total: 2, items: [ready, failed] })
    const wrapper = await mountView()
    await flushPromises()
    await wrapper.findAll('.capsule')[4].trigger('click') // 失败
    expect(wrapper.findAll('tbody tr')).toHaveLength(1)
    expect(wrapper.text()).toContain('合规制度库')
    expect(wrapper.text()).not.toContain('产品手册 v2')
  })

  it('名称搜索过滤列表', async () => {
    mockedFetch.mockResolvedValue({ page: 1, size: 100, total: 2, items: [ready, failed] })
    const wrapper = await mountView()
    await flushPromises()
    await wrapper.find('.el-input-stub').setValue('产品手册')
    expect(wrapper.findAll('tbody tr')).toHaveLength(1)
    expect(wrapper.text()).toContain('产品手册 v2')
  })

  it('重新向量化调用接口', async () => {
    mockedFetch.mockResolvedValue({ page: 1, size: 100, total: 1, items: [failed] })
    mockedReprocess.mockResolvedValue({ ...failed, status: 'PROCESSING' } as KnowledgeBaseDTO)
    const wrapper = await mountView()
    await flushPromises()
    // 绕过二次确认弹窗（ElMessageBox 在 jsdom 下以全局 mock 兜底）
    const { ElMessageBox } = await import('element-plus')
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm' as never)
    await wrapper.find('.link-btn').trigger('click')
    await flushPromises()
    expect(mockedReprocess).toHaveBeenCalledWith(2)
  })
})
