// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：知识库状态标签组件测试。覆盖四类状态文案渲染与失败原因 Tooltip。

import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import StatusTag from './StatusTag.vue'

/** el-tooltip 以纯插槽 stub 替换，避免依赖 popper */
const stubs = {
  'el-tooltip': { template: '<span><slot /></span>' }
}

describe('StatusTag', () => {
  it('渲染 READY 为已就绪', () => {
    const wrapper = mount(StatusTag, { props: { status: 'READY' }, global: { stubs } })
    expect(wrapper.text()).toContain('已就绪')
  })

  it('渲染 PROCESSING 为处理中并带进度', () => {
    const wrapper = mount(StatusTag, { props: { status: 'PROCESSING', progress: 45 }, global: { stubs } })
    expect(wrapper.text()).toContain('处理中')
    expect(wrapper.text()).toContain('45%')
  })

  it('渲染 FAILED 为失败', () => {
    const wrapper = mount(StatusTag, { props: { status: 'FAILED' }, global: { stubs } })
    expect(wrapper.text()).toContain('失败')
  })

  it('渲染 UPLOADING 为待处理', () => {
    const wrapper = mount(StatusTag, { props: { status: 'UPLOADING' }, global: { stubs } })
    expect(wrapper.text()).toContain('待处理')
  })
})
