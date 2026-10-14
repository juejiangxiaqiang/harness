// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：路由表与登录守卫。业务页面挂在 AppShell 壳下，路由懒加载分包；
// 除 login 外均要求登录，未登录访问携带原地址跳转登录页（对照计划书第 3 章）。

import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

/** 路由表：breadcrumb 用于 AppShell 面包屑，title 用于页面标题与文档标题 */
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/AppShell.vue'),
    children: [
      { path: '', redirect: '/knowledge-bases' },
      {
        path: 'knowledge-bases',
        name: 'knowledge-bases',
        component: () => import('@/views/KnowledgeBasesView.vue'),
        meta: { title: '知识库', breadcrumb: ['Harness 管理后台', '资源管理', '知识库'] }
      },
      {
        path: 'skills',
        name: 'skills',
        component: () => import('@/views/SkillsView.vue'),
        meta: { title: 'Skills', breadcrumb: ['Harness 管理后台', '资源管理', 'Skills'] }
      },
      {
        path: 'skills/new',
        name: 'skill-create',
        component: () => import('@/views/SkillCreateView.vue'),
        meta: { title: '新建 Skill', breadcrumb: ['Harness 管理后台', '资源管理', 'Skills', '新建 Skill'] }
      },
      {
        path: 'digital-employees',
        name: 'digital-employees',
        component: () => import('@/views/DigitalEmployeesView.vue'),
        meta: { title: '数字员工', breadcrumb: ['Harness 管理后台', '资源管理', '数字员工'] }
      },
      {
        path: 'digital-employees/new',
        name: 'employee-create',
        component: () => import('@/views/DigitalEmployeeCreateView.vue'),
        meta: { title: '新建数字员工', breadcrumb: ['Harness 管理后台', '资源管理', '数字员工', '新建数字员工'] }
      },
      {
        path: 'digital-employees/:id/preview',
        name: 'employee-preview',
        component: () => import('@/views/DigitalEmployeePreviewView.vue'),
        meta: { title: '数字员工预览', breadcrumb: ['Harness 管理后台', '资源管理', '数字员工', '预览'] }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/knowledge-bases' }
]

const router = createRouter({ history: createWebHistory(), routes })

// 登录守卫：public 路由放行；未登录访问受保护页跳登录页并记录原地址
router.beforeEach((to) => {
  const userStore = useUserStore()
  if (!to.meta.public && !userStore.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  // 已登录访问登录页直接回首页，避免来回跳转
  if (to.name === 'login' && userStore.isAuthenticated) {
    return { path: '/' }
  }
  return true
})

export default router
