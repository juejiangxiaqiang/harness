<!--
---
last_updated: 2026-10-08
status: active          # active | deprecated | draft
owner: @qiang
---
文件职责：App 外壳布局。侧边栏 + 顶部面包屑 + 内容区路由出口；
<768px 时侧边栏收起为抽屉图标栏（对照 VISUAL_DESIGN.md 3.1）。
-->
<script setup lang="ts">
// @ts-ignore - 解决 vue 模块声明文件未找到的类型检查问题
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Menu } from '@element-plus/icons-vue'
import SidebarMenu from './SidebarMenu.vue'

const route = useRoute()

/** 面包屑：取自路由 meta.breadcrumb，缺省回退为页面标题 */
const breadcrumb = computed(() => {
  const items = route.meta.breadcrumb as string[] | undefined
  return items && items.length ? items : ['Harness 管理后台', (route.meta.title as string) || '']
})

/** 抽屉侧边栏展开状态（窄屏时使用） */
const drawerOpen = ref(false)

/** 抽屉关闭（点击遮罩或菜单项跳转后） */
function onNav() {
  drawerOpen.value = false
}
</script>

<template>
  <div class="shell">
    <!-- 桌面端固定侧边栏 -->
    <SidebarMenu class="desktop-sidebar" />
    <!-- 窄屏抽屉：图标栏触发器 -->
    <el-drawer
      v-model="drawerOpen"
      direction="ltr"
      size="220px"
      :with-header="false"
      class="mobile-drawer"
    >
      <SidebarMenu @click="onNav" />
    </el-drawer>

    <main class="content">
      <header class="topbar">
        <button class="menu-trigger" aria-label="打开导航" @click="drawerOpen = true">
          <el-icon :size="18"><Menu /></el-icon>
        </button>
        <nav class="breadcrumb" aria-label="面包屑">
          <template v-for="(item, idx) in breadcrumb" :key="idx">
            <span :class="idx === breadcrumb.length - 1 ? 'crumb-current' : 'crumb-item'">
              {{ item }}
            </span>
            <span v-if="idx < breadcrumb.length - 1" class="crumb-sep">/</span>
          </template>
        </nav>
      </header>
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.shell {
  display: flex;
  height: 100%;
}

/* 桌面侧边栏固定 220px，独立纵向滚动，不随内容滚动 */
.desktop-sidebar {
  width: 220px;
  flex-shrink: 0;
  height: 100vh;
  position: sticky;
  top: 0;
}

.content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow-y: auto;
}

.topbar {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  height: 48px;
  background: var(--color-bg-surface);
  border-bottom: 1px solid var(--color-border-default);
  padding: 0 var(--space-4);
  flex-shrink: 0;
}

.menu-trigger {
  display: none;
  border: none;
  background: transparent;
  cursor: pointer;
  color: var(--color-text-secondary);
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-size-small);
  color: var(--color-text-secondary);
}

.crumb-current {
  color: var(--color-text-main);
}

.crumb-sep {
  color: var(--color-text-placeholder);
}

/* <768px：固定侧边栏隐藏，展示抽屉触发器 */
@media (max-width: 767px) {
  .desktop-sidebar {
    display: none;
  }
  .menu-trigger {
    display: inline-flex;
    align-items: center;
  }
}
</style>
