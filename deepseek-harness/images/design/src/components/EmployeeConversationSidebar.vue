<!-- 文件路径: design/src/components/EmployeeConversationSidebar.vue -->
<!-- 作用: 数字员工对话页会话侧栏 —— 品牌/新建会话/会话历史列表/用户，数据来自后端会话接口 -->
<template>
  <aside class="emp-conv-sidebar" :class="{ 'is-open': open }">
    <!-- 顶部：品牌 + 新建会话 -->
    <div class="sidebar-top">
      <BrandLogo />
      <button class="new-session-btn" @click="emit('new')">
        <el-icon :size="16"><Plus /></el-icon>
        新建会话
      </button>
    </div>

    <!-- 会话历史列表（滚动到底触发加载更多） -->
    <div ref="listRef" class="conv-list" @scroll="onListScroll">
      <p class="list-caption">会话历史</p>
      <div v-if="conversations.length === 0" class="empty-list">
        {{ loading ? '加载中…' : '暂无会话记录' }}
      </div>
      <div
        v-for="conv in conversations"
        :key="conv.id"
        class="conv-item"
        :class="{ 'is-active': conv.id === activeId }"
        @click="emit('select', conv.id)"
      >
        <span class="conv-title">{{ conv.title }}</span>
        <span class="conv-time">{{ formatTime(conv.createdAt) }}</span>
      </div>
      <div v-if="loading" class="list-more">加载中…</div>
      <div v-else-if="hasMore" class="list-more">下拉加载更多</div>
    </div>

    <!-- 底部：用户信息 + 退出 -->
    <div class="sidebar-bottom">
      <div class="user-row">
        <div class="user-avatar">{{ userNick()[0] ?? '我' }}</div>
        <span class="user-name">{{ userNick() }}</span>
        <button class="logout-btn" title="退出登录" @click="logout">
          <el-icon :size="16"><SwitchButton /></el-icon>
        </button>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
// 会话侧栏组件
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, SwitchButton } from '@element-plus/icons-vue'
import BrandLogo from '@/components/BrandLogo.vue'
import { logout as logoutApi } from '@/api/auth'
import { clearAuth, getUser } from '@/utils/storage'
import type { ConversationResponse } from '@/types'

// 会话列表数据与选中态
const props = defineProps<{
  conversations: ConversationResponse[]
  activeId: number | null
  loading: boolean
  hasMore: boolean
  open?: boolean
}>()

// 向外广播：新建/选中会话、加载更多
const emit = defineEmits<{
  (e: 'new'): void
  (e: 'select', id: number): void
  (e: 'load-more'): void
}>()

// 路由实例（退出登录跳转）
const router = useRouter()

// 列表滚动容器引用
const listRef = ref<HTMLElement | null>(null)

// 滚动到底部时触发加载更多
function onListScroll(): void {
  const el = listRef.value
  if (!el) return
  const nearBottom = el.scrollTop + el.clientHeight >= el.scrollHeight - 40
  if (nearBottom) {
    emit('load-more')
  }
}

// 当前登录用户昵称
function userNick(): string {
  return getUser()?.username ?? '用户'
}

// 格式化会话时间（今天显示时分，其余显示月日）
function formatTime(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  const pad = (n: number) => String(n).padStart(2, '0')
  const today = new Date()
  if (d.toDateString() === today.toDateString()) {
    return `${pad(d.getHours())}:${pad(d.getMinutes())}`
  }
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
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
/* 侧栏：固定宽度白底 */
.emp-conv-sidebar {
  display: flex;
  flex-direction: column;
  width: var(--chat-sidebar-width);
  flex-shrink: 0;
  background: var(--color-surface);
  border-right: 1px solid var(--color-border-light);
  height: 100%;
}

/* 顶部品牌区 */
.sidebar-top {
  padding: 12px 12px 4px;
}

/* 新建会话按钮：黑色强调 */
.new-session-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 36px;
  margin: 12px;
  border: none;
  border-radius: var(--radius-btn);
  background: var(--color-black);
  color: var(--color-text-invert);
  font-size: var(--font-size-body);
  cursor: pointer;
  transition: background var(--duration-base) var(--ease-out);
}
.new-session-btn:hover {
  background: var(--color-black-hover);
}

/* 会话历史列表 */
.conv-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 12px 12px;
}

/* 区块标题 */
.list-caption {
  padding: 8px 4px 4px;
  font-size: var(--font-size-caption);
  color: var(--color-text-muted);
}

/* 空态 */
.empty-list {
  padding: 24px 0;
  text-align: center;
  font-size: var(--font-size-caption);
  color: var(--color-text-muted);
}

/* 会话项 */
.conv-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 12px;
  margin-bottom: 2px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--duration-base) var(--ease-out);
}
.conv-item:hover {
  background: var(--color-border-light);
}
/* 选中态：浅蓝底 + 蓝色描边 */
.conv-item.is-active {
  background: var(--color-primary-lighter);
  outline: 1px solid var(--color-primary-light);
}

/* 会话标题 */
.conv-title {
  font-size: var(--font-size-body);
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.conv-item.is-active .conv-title {
  color: var(--color-primary);
  font-weight: var(--font-weight-medium);
}

/* 会话时间 */
.conv-time {
  font-size: var(--font-size-caption);
  color: var(--color-text-muted);
}

/* 加载更多提示 */
.list-more {
  padding: 8px 0;
  text-align: center;
  font-size: var(--font-size-caption);
  color: var(--color-text-muted);
}

/* 底部用户区 */
.sidebar-bottom {
  padding: 12px;
  border-top: 1px solid var(--color-border-light);
}

/* 用户行 */
.user-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 4px;
}

/* 用户头像 */
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

/* 移动端（≤768px）：侧栏以抽屉形式覆盖在对话区上，默认隐藏，点击头部按钮展开 */
@media (max-width: 767.98px) {
  .emp-conv-sidebar {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    z-index: 100;
    transform: translateX(-100%);
    box-shadow: var(--shadow-popover);
    transition: transform var(--duration-base) var(--ease-out);
  }
  .emp-conv-sidebar.is-open {
    transform: none;
  }
}
</style>
