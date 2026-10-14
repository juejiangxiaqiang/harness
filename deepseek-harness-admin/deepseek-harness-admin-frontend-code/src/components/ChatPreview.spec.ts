// ---
// last_updated: 2026-10-08
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：对话验证组件测试。Mock 对话接口，覆盖发送消息、空输入拦截与快捷提问链路。

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ChatPreview from './ChatPreview.vue'
import { sendChatMessage } from '@/api/previewChat'
import type { ChatResponseDTO } from '@/types'

// Mock 对话与历史接口，避免真实网络请求
vi.mock('@/api/previewChat', () => ({
  sendChatMessage: vi.fn(),
  fetchConversations: vi.fn().mockResolvedValue({ items: [] }),
  fetchMessages: vi.fn().mockResolvedValue([])
}))

const reply: ChatResponseDTO = {
  conversationId: 10,
  reply: '根据知识库，售后流程为先登记后跟进。',
  createdAt: '2026-10-08T10:00:00'
}

describe('ChatPreview', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(sendChatMessage).mockResolvedValue(reply)
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('发送消息后展示用户气泡与接口回复', async () => {
    const wrapper = mount(ChatPreview, {
      props: { employeeId: 2, employeeName: '制度问答专家' },
      global: { stubs: { 'el-icon': { template: '<span><slot /></span>' } } }
    })
    await flushPromises()
    await wrapper.find('textarea').setValue('你好')
    await wrapper.find('.send-btn').trigger('click')
    // 用户消息立即出现，且使用当前员工 ID 调用对话接口发起新会话
    expect(wrapper.text()).toContain('你好')
    expect(sendChatMessage).toHaveBeenCalledWith(2, { message: '你好', conversationId: undefined })
    expect(wrapper.find('.typing-indicator').exists()).toBe(true)
    // 回复返回后展示助手气泡并停止指示器
    await flushPromises()
    expect(wrapper.find('.typing-indicator').exists()).toBe(false)
    expect(wrapper.findAll('.msg-row')).toHaveLength(2)
    expect(wrapper.text()).toContain('根据知识库，售后流程为先登记后跟进。')
  })

  it('空输入不发送', async () => {
    const wrapper = mount(ChatPreview, {
      props: { employeeId: 2 },
      global: { stubs: { 'el-icon': { template: '<span><slot /></span>' } } }
    })
    await flushPromises()
    await wrapper.find('.send-btn').trigger('click')
    expect(wrapper.findAll('.msg-row')).toHaveLength(0)
    expect(sendChatMessage).not.toHaveBeenCalled()
  })

  it('快捷提问直接发送问题', async () => {
    const wrapper = mount(ChatPreview, {
      props: { employeeId: 2 },
      global: { stubs: { 'el-icon': { template: '<span><slot /></span>' } } }
    })
    await flushPromises()
    await wrapper.findAll('.quick-pill')[0].trigger('click')
    expect(wrapper.text()).toContain('帮我介绍一下岗位职责')
    expect(sendChatMessage).toHaveBeenCalledWith(2, { message: '帮我介绍一下岗位职责', conversationId: undefined })
    await flushPromises()
    expect(wrapper.findAll('.msg-row')).toHaveLength(2)
  })
})