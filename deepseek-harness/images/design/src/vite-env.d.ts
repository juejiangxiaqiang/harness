// 文件路径: design/src/vite-env.d.ts
// 作用: 引入 Vite 客户端类型，并声明 .vue 单文件组件模块类型
/// <reference types="vite/client" />

// 声明 .vue 文件默认导出一个组件对象，供 TypeScript 识别
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export default component
}
