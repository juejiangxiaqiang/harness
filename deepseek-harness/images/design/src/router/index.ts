// 文件路径: design/src/router/index.ts
// 作用: 路由表 —— 登录/注册/对话/数字员工列表/数字员工对话
import { createRouter, createWebHistory } from 'vue-router'
// 登录页
import LoginView from '@/views/LoginView.vue'
// 注册页
import RegisterView from '@/views/RegisterView.vue'
// deepseek harness 对话页
import ChatView from '@/views/ChatView.vue'
// 数字员工列表页
import EmployeesView from '@/views/EmployeesView.vue'
// 数字员工对话页
import EmployeeChatView from '@/views/EmployeeChatView.vue'

// 定义路由表
const routes = [
  // 根路径跳转到登录页
  { path: '/', redirect: '/login' },
  // 用户登录入口
  { path: '/login', name: 'Login', component: LoginView, meta: { public: true } },
  // 用户注册入口
  { path: '/register', name: 'Register', component: RegisterView, meta: { public: true } },
  // deepseek harness 对话页
  { path: '/chat', name: 'Chat', component: ChatView },
  // 数字员工列表页
  { path: '/employees', name: 'Employees', component: EmployeesView },
  // 数字员工对话页（携带员工 id）
  { path: '/employee/:id', name: 'EmployeeChat', component: EmployeeChatView }
]

// 创建路由实例
const router = createRouter({
  // 使用 HTML5 history 模式
  history: createWebHistory(),
  // 注册路由表
  routes
})

// 全局前置守卫：未登录时跳转登录页（登录/注册页除外）
router.beforeEach((to) => {
  // 判断是否已登录（localStorage 中存在登录凭证）
  const loggedIn = !!localStorage.getItem('ssms:token')
  // 目标页要求登录但当前未登录，则重定向到登录页
  if (!to.meta.public && !loggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  // 已登录访问登录/注册页时，跳回对话页
  if (loggedIn && to.meta.public) {
    return { path: '/chat' }
  }
  // 其余情况放行
  return true
})

// 导出路由实例
export default router
