<!-- 文件路径: design/src/components/AppSidebar.vue -->
<!-- 作用: 数字员工页左侧导航栏 —— 品牌区/主导航/管理驾驶舱/底部用户，对齐参考图侧边栏 -->
<template>
  <aside class="app-sidebar">
    <!-- 顶部品牌区 -->
    <BrandLogo show-subtitle />

    <!-- 主导航菜单 -->
    <nav class="nav-menu">
      <button
        v-for="item in navMenus"
        :key="item.key"
        class="nav-item"
        :class="{ 'is-active': isActive(item.key) }"
        @click="onNavClick(item.key)"
      >
        <el-icon :size="18"><component :is="menuIcon(item.key)" /></el-icon>
        <span>{{ item.label }}</span>
      </button>
    </nav>

    <!-- 底部用户信息 -->
    <div class="sidebar-footer">
      <div class="user-row">
        <!-- 用户头像：昵称首字圆形头像 -->
        <div class="user-avatar">{{ userNick()[0] ?? '我' }}</div>
        <span class="user-name">{{ userNick() }}</span>
        <!-- 退出登录 -->
        <el-tooltip content="退出登录" placement="top">
          <button class="logout-btn" title="退出登录" @click="logout">
            <el-icon :size="16"><SwitchButton /></el-icon>
          </button>
        </el-tooltip>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
// 数字员工页左侧导航
import { useRoute, useRouter } from 'vue-router'
import type { Component } from 'vue'
import { Avatar, Clock, SwitchButton } from '@element-plus/icons-vue'
import BrandLogo from '@/components/BrandLogo.vue'
import { navMenus } from '@/mock/employees'
import { logout as logoutApi } from '@/api/auth'
import { clearAuth, getUser } from '@/utils/storage'

// 当前路由（用于高亮）
const route = useRoute()
// 路由实例（用于跳转）
const router = useRouter()

// 菜单图标映射
const icons: Record<string, Component> = {
  clock: Clock,
  avatar: Avatar
}

// 判断菜单是否选中
function isActive(key: string): boolean {
  if (key === 'employees') return route.path.startsWith('/employee')
  return false
}

// 获取菜单图标组件
function menuIcon(key: string): Component {
  return icons[key]
}

// 当前登录用户昵称（后端无昵称，展示用户名）
function userNick(): string {
  return getUser()?.username ?? '用户'
}

// 菜单点击分发
function onNavClick(key: string): void {
  // 新建任务进入 deepseek harness 对话页
  if (key === 'new-task') router.push('/chat')
  // 数字员工进入能力中心
  else if (key === 'employees') router.push('/employees')
}

// 退出登录：先调接口吊销凭证，失败也清理本地，保证可登出
async function logout(): Promise<void> {
  try {
    await logoutApi()
  } catch {
    // 接口失败不阻塞登出
  }
  clearAuth()
  router.push('/login')
}
</script>

<style scoped>
/* 侧边栏：固定宽 200px，白底 */
.app-sidebar {
  display: flex;
  flex-direction: column;
  width: var(--sidebar-width);
  flex-shrink: 0;
  background: var(--color-surface);
  border-right: 1px solid var(--color-border-light);
}

/* 导航菜单 */
.nav-menu {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 12px;
  flex: 1;
}

/* 导航项：圆角 8px、左右内边距 12px、上下 8px */
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border: none;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-text-secondary);
  font-size: var(--font-size-body);
  text-align: left;
  cursor: pointer;
  transition:
    background var(--duration-base) var(--ease-out),
    color var(--duration-base) var(--ease-out);
}
/* 悬停态 */
.nav-item:hover {
  background: var(--color-border-light);
  color: var(--color-text);
}
/* 选中态：灰色圆角背景高亮 */
.nav-item.is-active {
  background: var(--color-border-light);
  color: var(--color-text);
  font-weight: var(--font-weight-medium);
}

/* 底部用户区 */
.sidebar-footer {
  padding: 12px;
  border-top: 1px solid var(--color-border-light);
}

/* 用户行 */
.user-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 用户头像：圆形 */
.user-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--color-primary);
  color: var(--color-text-invert);
  font-size: var(--font-size-body);
  font-weight: var(--font-weight-medium);
}

/* 用户名 */
.user-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--font-size-body);
  color: var(--color-text);
}

/* 退出按钮 */
.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  transition: all var(--duration-base) var(--ease-out);
}
.logout-btn:hover {
  background: var(--color-border-light);
  color: var(--color-danger);
}

/* 移动端（≤768px）：侧栏压缩为 64px 图标模式，仅保留图标与头像 */
@media (max-width: 767.98px) {
  .app-sidebar {
    width: 64px;
  }
  .app-sidebar .nav-item span,
  .app-sidebar .user-name {
    display: none;
  }
  .app-sidebar .nav-item {
    justify-content: center;
    padding: 10px 0;
  }
  /* 子组件 BrandLogo 内的文字需 :deep 穿透 scoped 作用域 */
  .app-sidebar :deep(.brand-title),
  .app-sidebar :deep(.brand-subtitle) {
    display: none;
  }
  .app-sidebar .user-row {
    justify-content: center;
    gap: 0;
  }
}
</style>
