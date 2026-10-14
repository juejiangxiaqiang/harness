// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：应用入口。装配 Pinia、Router、Element Plus 中文语言包与全局样式，
// 并注册全局 401 回调（清理会话 + 跳转登录页）。

// @ts-ignore - 解决 vue 模块声明文件未找到的类型检查问题
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { setUnauthorizedHandler } from './api/apiClient'
import { useUserStore } from './stores/user'
import './assets/tokens.css'
import './assets/main.css'

const app = createApp(App)
app.use(createPinia())

// 全局 401 处理：清理会话并携带原地址跳转登录页，便于登录后回跳
setUnauthorizedHandler(() => {
  const userStore = useUserStore()
  userStore.clearSession()
  const current = router.currentRoute.value.fullPath
  if (current !== '/login') {
    router.push({ name: 'login', query: { redirect: current } })
  }
})

app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
