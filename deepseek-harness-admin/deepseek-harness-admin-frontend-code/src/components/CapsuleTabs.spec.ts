// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：胶囊筛选组件测试。覆盖激活态展示与选择回传事件。

import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import CapsuleTabs from './CapsuleTabs.vue'

const options = [
  { value: 'ALL', label: '全部' },
  { value: 'READY', label: '已就绪' },
  { value: 'FAILED', label: '失败' }
]

describe('CapsuleTabs', () => {
  it('按传入 modelValue 展示激活态', () => {
    const wrapper = mount(CapsuleTabs, { props: { options, modelValue: 'READY' } })
    const active = wrapper.find('.capsule.active')
    expect(active.text()).toBe('已就绪')
  })

  it('点击选项回传新值', async () => {
    const wrapper = mount(CapsuleTabs, { props: { options, modelValue: 'ALL' } })
    const buttons = wrapper.findAll('.capsule')
    await buttons[2].trigger('click')
    expect(wrapper.emitted('update:modelValue')).toEqual([['FAILED']])
  })
})
