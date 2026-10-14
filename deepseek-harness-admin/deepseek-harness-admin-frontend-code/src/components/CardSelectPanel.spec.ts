// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：卡片多选面板组件测试。覆盖勾选切换、禁用项拦截、搜索过滤与计数。

import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { ElIcon, ElInput, ElTooltip } from 'element-plus'
import CardSelectPanel from './CardSelectPanel.vue'

const items = [
  { id: 1, label: '产品手册', subtitle: 'v2', icon: '产' },
  { id: 2, label: '合规制度库', subtitle: 'docx', disabled: true, disabledTip: '仅 READY 状态知识库可关联' },
  { id: 3, label: '授信指引', subtitle: 'md' }
]

/** 未在插件中注册的 el-* 组件：直接以真实组件注入，保证 v-model 生效 */
const epComponents = { ElInput, ElTooltip, ElIcon }

describe('CardSelectPanel', () => {
  it('渲染条目、禁用态与已选计数', () => {
    const wrapper = mount(CardSelectPanel, {
      props: { title: '关联知识库', items, modelValue: [1] },
      global: { components: epComponents }
    })
    expect(wrapper.findAll('.panel-item')).toHaveLength(3)
    expect(wrapper.text()).toContain('已选 1/3')
    expect(wrapper.find('.panel-item.disabled').exists()).toBe(true)
  })

  it('点击可选条目回传新增的 ID 列表', async () => {
    const wrapper = mount(CardSelectPanel, {
      props: { title: '关联知识库', items, modelValue: [1] },
      global: { components: epComponents }
    })
    await wrapper.findAll('.panel-item')[2].trigger('click')
    // 首条调用记录的第一个参数即本次勾选结果
    const calls = wrapper.emitted('update:modelValue')
    expect(calls?.[0]?.[0]).toEqual([1, 3])
  })

  it('点击禁用条目不回传', async () => {
    const wrapper = mount(CardSelectPanel, {
      props: { title: '关联知识库', items, modelValue: [] },
      global: { components: epComponents }
    })
    await wrapper.find('.panel-item.disabled').trigger('click')
    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
  })

  it('搜索关键字过滤条目', async () => {
    const wrapper = mount(CardSelectPanel, {
      props: { title: '关联知识库', items, modelValue: [] },
      global: { components: epComponents }
    })
    // 真实 el-input 内部渲染为 input.el-input__inner
    await wrapper.find('input').setValue('产品')
    const rows = wrapper.findAll('.panel-item')
    expect(rows).toHaveLength(1)
    expect(rows[0].text()).toContain('产品手册')
  })
})
