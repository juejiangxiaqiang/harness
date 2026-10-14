// 文件路径: design/src/main.ts
// 作用: 应用入口 —— 创建 Vue 实例、注册路由与 Element Plus、载入全局样式
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
// Element Plus 基础样式
import 'element-plus/dist/index.css'
// 全局设计令牌与基础样式
import './styles/tokens.css'
import './styles/base.css'
// 路由实例
import router from './router'
// 根组件
import App from './App.vue'

// 创建应用实例
const app = createApp(App)

// 注册路由
app.use(router)

// 注册 Element Plus 插件（含全量组件）
app.use(ElementPlus)

// 挂载到 #app 节点
app.mount('#app')
