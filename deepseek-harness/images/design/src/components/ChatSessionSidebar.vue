<!-- 文件路径: design/src/components/ChatSessionSidebar.vue -->
<!-- 作用: deepseek harness 对话页会话侧栏 —— 品牌/新建会话/历史记录列表/设置/用户，保留原页布局结构 -->
<template>
  <aside class="chat-sidebar" :class="{ 'is-open': open }">
    <!-- 顶部：品牌 + 数字员工入口 -->
    <div class="sidebar-top">
      <BrandLogo />
      <!-- 返回数字员工能力中心 -->
      <router-link class="emp-entry" to="/employees">
        <el-icon :size="14"><Avatar /></el-icon>
        <span>数字员工</span>
      </router-link>
    </div>

    <!-- 新建会话按钮 -->
    <button class="new-session-btn" @click="chat.createSession()">
      <el-icon :size="16"><Plus /></el-icon>
      新建会话
    </button>

    <!-- 会话搜索 -->
    <el-input v-model="keyword" class="session-search" placeholder="搜索历史记录" clearable>
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
    </el-input>

    <!-- 历史记录列表 -->
    <div class="session-list">
      <p class="list-caption">历史记录</p>
      <div v-if="filteredSessions.length === 0" class="empty-list">暂无会话记录</div>
      <div
        v-for="session in filteredSessions"
        :key="session.id"
        class="session-item"
        :class="{ 'is-active': session.id === chat.activeId.value }"
        @click="chat.selectSession(session.id)"
      >
        <!-- 会话标题 -->
        <span class="session-title">{{ session.title }}</span>
        <!-- 会话时间 -->
        <span class="session-time">{{ formatTime(session.updatedAt) }}</span>
        <!-- 悬停操作：重命名/删除 -->
        <span class="session-actions" @click.stop>
          <button class="act-btn" title="重命名" @click="rename(session.id)">
            <el-icon :size="13"><EditPen /></el-icon>
          </button>
          <button class="act-btn danger" title="删除" @click="remove(session.id)">
            <el-icon :size="13"><Delete /></el-icon>
          </button>
        </span>
      </div>
    </div>

    <!-- 底部：设置 + 用户 -->
    <div class="sidebar-bottom">
      <!-- 设置入口 -->
      <button class="settings-btn" title="对话设置" @click="emit('open-settings')">
        <el-icon :size="16"><Setting /></el-icon>
        <span>设置</span>
      </button>
      <!-- 用户信息 + 退出 -->
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
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  Avatar,
  Delete,
  EditPen,
  Plus,
  Search,
  Setting,
  SwitchButton
} from '@element-plus/icons-vue'
import BrandLogo from '@/components/BrandLogo.vue'
import type { UseChat } from '@/composables/useChat'
import { logout as logoutApi } from '@/api/auth'
import { clearAuth, getUser } from '@/utils/storage'

// 对话逻辑对象（由父页面通过 useChat 创建后传入）
const props = defineProps<{ chat: UseChat; open?: boolean }>()

// 向外广播“打开设置”
const emit = defineEmits<{ (e: 'open-settings'): void }>()

// 路由实例（用于跳转）
const router = useRouter()

// 会话搜索关键词
const keyword = ref('')

// 按关键词过滤会话
const filteredSessions = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return props.chat.sessions.value
  return props.chat.sessions.value.filter((s) => s.title.toLowerCase().includes(kw))
})

// 当前登录用户昵称（后端无昵称，展示用户名）
function userNick(): string {
  return getUser()?.username ?? '用户'
}

// 重命名会话（弹窗输入）
async function rename(id: string): Promise<void> {
  try {
    const { value } = await ElMessageBox.prompt('请输入新的会话名称', '重命名会话', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: props.chat.sessions.value.find((s) => s.id === id)?.title ?? ''
    })
    props.chat.renameSession(id, value)
  } catch {
    // 用户取消重命名，不处理
  }
}

// 删除会话（二次确认）
async function remove(id: string): Promise<void> {
  try {
    await ElMessageBox.confirm('删除后不可恢复，确定删除该会话吗？', '删除会话', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    props.chat.removeSession(id)
  } catch {
    // 用户取消删除，不处理
  }
}

// 格式化会话时间（今天显示时分，其余显示月日）
function formatTime(ts: number): string {
  const d = new Date(ts)
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
.chat-sidebar {
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

/* 数字员工入口 */
.emp-entry {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-left: 12px;
  font-size: var(--font-size-caption);
  color: var(--color-primary);
  transition: color var(--duration-base);
}
.emp-entry:hover {
  color: var(--color-primary-hover);
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

/* 会话搜索 */
.session-search {
  margin: 0 12px 8px;
}

/* 历史记录列表 */
.session-list {
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
.session-item {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 12px;
  margin-bottom: 2px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--duration-base) var(--ease-out);
}
.session-item:hover {
  background: var(--color-border-light);
}
/* 选中态：浅蓝底 + 蓝色描边 */
.session-item.is-active {
  background: var(--color-primary-lighter);
  outline: 1px solid var(--color-primary-light);
}

/* 会话标题 */
.session-title {
  font-size: var(--font-size-body);
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.session-item.is-active .session-title {
  color: var(--color-primary);
  font-weight: var(--font-weight-medium);
}

/* 会话时间 */
.session-time {
  font-size: var(--font-size-caption);
  color: var(--color-text-muted);
}

/* 悬停操作按钮组 */
.session-actions {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  display: none;
  gap: 2px;
}
.session-item:hover .session-actions {
  display: flex;
}

/* 操作按钮 */
.act-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-secondary);
  cursor: pointer;
  box-shadow: var(--shadow-card);
  transition: color var(--duration-base);
}
.act-btn:hover {
  color: var(--color-primary);
}
.act-btn.danger:hover {
  color: var(--color-danger);
}

/* 底部操作区 */
.sidebar-bottom {
  padding: 12px;
  border-top: 1px solid var(--color-border-light);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 设置按钮 */
.settings-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: none;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-text-secondary);
  font-size: var(--font-size-body);
  cursor: pointer;
  transition: all var(--duration-base) var(--ease-out);
}
.settings-btn:hover {
  background: var(--color-border-light);
  color: var(--color-text);
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
  .chat-sidebar {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    z-index: 100;
    transform: translateX(-100%);
    box-shadow: var(--shadow-popover);
    transition: transform var(--duration-base) var(--ease-out);
  }
  .chat-sidebar.is-open {
    transform: none;
  }
}
</style>
