<!--
---
last_updated: 2026-10-08
status: active          # active | deprecated | draft
owner: @qiang
---
文件职责：侧边栏。品牌区 + 资源管理菜单分组 + 底部用户区；
选中态浅蓝底 + 左侧 3px 主色条（对照 VISUAL_DESIGN.md 3.2）。
-->
<script setup lang="ts">
// @ts-ignore - 解决 vue 模块声明文件未找到的类型检查问题
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Collection, MagicStick, User, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

// 侧边栏导航项（对照 VISUAL_DESIGN.md 3.2）：当前路由未覆盖"发布管理"独立页面，
// 故仅展示资源管理三项，样式与交互对齐设计稿
const navItems = [
  { path: '/knowledge-bases', label: '知识库', icon: Collection },
  { path: '/skills', label: 'Skills', icon: MagicStick },
  { path: '/digital-employees', label: '数字员工', icon: User }
]

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 当前用户显示名：优先真实账号，无会话时占位 */
const displayName = computed(() => userStore.user?.username || '管理员')

/** 头像首字：取用户名首字母大写 */
const avatarChar = computed(() => displayName.value.charAt(0).toUpperCase())

/** 退出登录：二次确认后调 store.logout 并跳转登录页 */
async function handleLogout() {
  await ElMessageBox.confirm('确定要退出当前账号吗？', '退出登录', {
    confirmButtonText: '退出',
    cancelButtonText: '取消',
    type: 'warning'
  }).catch(() => null)
  try {
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push({ name: 'login' })
  } catch {
    // 后端吊销失败时 store 已清理本地会话，同样可安全跳转
    router.push({ name: 'login' })
  }
}
</script>

<template>
  <aside class="sidebar">
    <div class="brand">
      <div class="brand-logo" aria-hidden="true"></div>
      <div class="brand-text">
        <div class="brand-name">Harness 管理后台</div>
        <div class="brand-sub">数字员工</div>
      </div>
    </div>

    <nav class="menu" aria-label="主导航">
      <div class="menu-group">资源管理</div>
      <router-link
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="menu-item"
        :class="{ active: route.path === item.path }"
        :aria-current="route.path === item.path ? 'page' : undefined"
      >
        <el-icon :size="16"><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
      </router-link>
    </nav>

    <div class="user-area">
      <div class="avatar">{{ avatarChar }}</div>
      <span class="user-name">{{ displayName }}</span>
      <el-tooltip content="退出登录" placement="top">
        <button class="logout-btn" aria-label="退出登录" @click="handleLogout">
          <el-icon :size="16"><SwitchButton /></el-icon>
        </button>
      </el-tooltip>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 220px;
  flex-shrink: 0;
  background: var(--color-bg-surface);
  border-right: 1px solid var(--color-border-default);
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* 品牌区：几何三角形 Logo（主色渐变）+ 名称 + 副标签，无下边框 */
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 16px 16px;
}

.brand-logo {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, var(--color-primary), var(--color-accent-blue));
  clip-path: polygon(50% 0, 100% 100%, 0 100%);
}

.brand-name {
  font-size: var(--font-size-card);
  font-weight: var(--font-weight-card);
  color: var(--color-text-main);
}

.brand-sub {
  font-size: var(--font-size-small);
  color: var(--color-text-placeholder);
}

.menu {
  flex: 1;
  padding: 8px 0;
  overflow-y: auto;
}

.menu-group {
  font-size: var(--font-size-small);
  color: var(--color-text-placeholder);
  padding: 12px 20px 6px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 2px 12px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  text-decoration: none;
  font-size: var(--font-size-body);
  position: relative;
  transition: var(--transition-base);
}

.menu-item:hover {
  background: var(--color-bg-hover);
  color: var(--color-text-main);
}

/* 选中态：浅蓝底 + 文字主色 + 左侧 3px 指示条（含细分路径） */
.menu-item.active {
  background: var(--color-primary-bg);
  color: var(--color-primary);
  font-weight: var(--font-weight-card);
}

.menu-item.active::before {
  content: '';
  position: absolute;
  left: -12px;
  top: 8px;
  bottom: 8px;
  width: 3px;
  border-radius: 2px;
  background: var(--color-primary);
}

/* 分组分隔线 */
.nav-sep {
  height: 1px;
  background: var(--color-border-default);
  margin: 8px 16px;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-top: 1px solid var(--color-border-default);
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary), var(--color-accent-blue));
  color: #fff;
  font-weight: var(--font-weight-card);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-body);
}

.user-name {
  flex: 1;
  font-size: var(--font-size-body);
  font-weight: var(--font-weight-card);
  color: var(--color-text-main);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 退出按钮：默认灰字，hover 变危险红 */
.logout-btn {
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  padding: 4px;
  border-radius: var(--radius-sm);
  transition: var(--transition-base);
  display: inline-flex;
}

.logout-btn:hover {
  color: var(--color-danger);
}
</style>
