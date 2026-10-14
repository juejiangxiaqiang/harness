// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：Vitest 测试环境准备。补齐 jsdom 缺失的 ResizeObserver/matchMedia，
// 禁用 transition 全局 stub，保证 Element Plus 相关组件可稳定挂载测试。

import { config } from '@vue/test-utils'

// Element Plus 的部分组件（如 el-slider/el-tooltip）依赖 ResizeObserver，jsdom 未实现
class ResizeObserverMock {
  observe() {}
  unobserve() {}
  disconnect() {}
}
globalThis.ResizeObserver = ResizeObserverMock

// window.matchMedia 用于组件库的响应式判断，jsdom 缺省实现为空匹配
if (!window.matchMedia) {
  window.matchMedia = (query: string) =>
    ({
      matches: false,
      media: query,
      onchange: null,
      addListener() {},
      removeListener() {},
      addEventListener() {},
      removeEventListener() {},
      dispatchEvent() {
        return false
      }
    }) as unknown as MediaQueryList
}

// jsdom 对 transition 动画支持不佳，全局关闭以避免告警与异步残留
config.global.stubs = {
  transition: false,
  'transition-group': false
}
