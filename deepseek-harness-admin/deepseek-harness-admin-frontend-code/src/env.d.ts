// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：为 .vue 与 import.meta.env 提供类型声明，保证严格模式编译通过。

/// <reference types="vite/client" />

declare module '*.vue' {
// @ts-ignore - 解决 vue 模块声明文件未找到的类型检查问题
  import type { DefineComponent } from 'vue'
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const component: DefineComponent<Record<string, any>, Record<string, any>, any>
  export default component
}

interface ImportMetaEnv {
  /** API 基础路径，默认 /api，开发与生产经反向代理统一处理 */
  readonly VITE_API_BASE_URL: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
