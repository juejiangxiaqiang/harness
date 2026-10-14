<!-- 文件路径: design/src/views/EmployeeChatView.vue -->
<!-- 作用: 数字员工对话页 —— 从列表页点击卡片进入，与指定数字员工对话
     会话历史/消息/对话对接后端接口（docs/2.0/前端改造计划文档.md §3.4），风格与整体体系统一 -->
<template>
  <div class="employee-chat-page">
    <!-- 移动端抽屉展开时的点击遮罩 -->
    <div v-if="sidebarOpen" class="drawer-mask" @click="sidebarOpen = false" />

    <!-- 左侧导航栏 -->
    <AppSidebar />

    <!-- 会话侧栏（后端会话历史） -->
    <EmployeeConversationSidebar
      :conversations="chat.conversations.value"
      :active-id="chat.activeConversationId.value"
      :loading="chat.convLoading.value"
      :has-more="chat.hasMoreConversations()"
      :open="sidebarOpen"
      @new="chat.startNewConversation()"
      @select="onSelectConversation"
      @load-more="chat.loadConversations()"
    />

    <!-- 对话区 -->
    <main class="chat-main">
      <!-- 顶部标题栏 -->
      <header class="chat-header">
        <!-- 移动端：展开会话侧栏抽屉 -->
        <button class="mobile-menu-btn" title="会话列表" @click="sidebarOpen = !sidebarOpen">
          <el-icon :size="18"><Menu /></el-icon>
        </button>
        <!-- 返回数字员工列表 -->
        <button class="back-btn" title="返回数字员工列表" @click="router.push('/employees')">
          <el-icon :size="16"><ArrowLeft /></el-icon>
        </button>
        <!-- 员工头像 -->
        <EmployeeAvatar v-if="employee" :char="employee.iconChar" :color="employee.color" :size="40" />
        <div class="header-info">
          <div class="header-title-row">
            <h2 class="header-title">{{ employee?.name ?? '数字员工' }}</h2>
            <!-- 分类标签（后端无分类字段时隐藏） -->
            <span v-if="employee?.category" class="category-tag">{{ employee.category }}</span>
          </div>
          <p class="header-intro">{{ employee?.desc ?? '' }}</p>
        </div>
        <!-- 状态角标（按发布状态派生） -->
        <span class="status-pill" :class="statusClass"><i />{{ statusText }}</span>
      </header>

      <!-- 消息滚动区 -->
      <div ref="scrollRef" class="chat-messages">
        <!-- 空态欢迎区 -->
        <div v-if="messages.length === 0" class="empty-hero">
          <EmployeeAvatar v-if="employee" :char="employee.iconChar" :color="employee.color" :size="56" />
          <h3 class="hero-title">{{ employee?.name ?? '数字员工' }}</h3>
          <p class="hero-intro">{{ employee?.desc ?? '你好，我可以帮你处理相关事务。' }}试试这些快捷问题：</p>
          <!-- 快捷问题 -->
          <div class="suggestions">
            <button
              v-for="s in suggestions"
              :key="s"
              class="suggestion-chip"
              :disabled="chat.sending.value"
              @click="onSend(s)"
            >
              {{ s }}
            </button>
          </div>
        </div>

        <!-- 消息列表 -->
        <template v-else>
          <MessageItem
            v-for="(msg, index) in messages"
            :key="msg.id"
            :message="msg"
            :streaming="chat.isStreaming.value && index === messages.length - 1 && msg.role === 'assistant'"
            :avatar-char="employee?.iconChar ?? '员'"
            :avatar-color="employee?.color ?? '#2563eb'"
          />
        </template>
      </div>

      <!-- 底部输入区 -->
      <div class="chat-composer-wrap">
        <ChatComposer
          :streaming="chat.isStreaming.value"
          placeholder="向数字员工提问…"
          @send="onSend"
          @stop="chat.stopStreaming()"
        />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
// 数字员工对话页逻辑
import { computed, onMounted, ref, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Menu } from '@element-plus/icons-vue'
import AppSidebar from '@/components/AppSidebar.vue'
import EmployeeConversationSidebar from '@/components/EmployeeConversationSidebar.vue'
import MessageItem from '@/components/MessageItem.vue'
import ChatComposer from '@/components/ChatComposer.vue'
import EmployeeAvatar from '@/components/EmployeeAvatar.vue'
import { useEmployeeChat } from '@/composables/useEmployeeChat'
import { toEmployeeCard } from '@/api/employees'
import type { Employee } from '@/types'

// 当前路由参数（员工 id）
const route = useRoute()
// 路由实例
const router = useRouter()

// 员工 id（路由参数）
const empId = computed(() => String(route.params.id ?? ''))

// 对话核心逻辑（会话数据来自后端接口）
const chat = useEmployeeChat(empId.value)
// 当前会话消息（模板使用）
const messages = chat.messages

// 卡片视图模型（头部展示头像/标题/描述）
const employee = computed<Employee | null>(() => {
  const detail = chat.detail.value
  return detail ? toEmployeeCard(detail) : null
})

// 状态角标：按发布状态派生文案与样式
const statusText = computed(() => {
  const status = chat.detail.value?.status
  if (status === 'PUBLISHED') return '在线'
  if (status === 'DRAFT') return '草稿'
  return '已归档'
})
const statusClass = computed(() => {
  const status = chat.detail.value?.status
  return status === 'PUBLISHED' ? 'is-online' : 'is-offline'
})

// 消息滚动容器引用
const scrollRef = ref<HTMLElement | null>(null)
// 移动端会话侧栏抽屉可见性
const sidebarOpen = ref(false)

// 快捷问题（通用示例）
const suggestions = [
  '介绍一下你的能力',
  '今天有什么进展可以汇报',
  '帮我分析一组数据',
  '生成一份处理建议'
]

// 页面挂载时：校验员工存在并加载会话历史
onMounted(async () => {
  try {
    await chat.loadDetail()
  } catch (err) {
    // 员工不存在或无权访问：返回列表并提示
    ElMessage.error(err instanceof Error ? err.message : '数字员工不存在')
    router.replace('/employees')
    return
  }
  void chat.loadConversations()
})

// 移动端：切换会话后自动收起侧栏抽屉
watch(
  () => chat.activeConversationId.value,
  () => {
    sidebarOpen.value = false
  }
)

// 选中会话：拉取该会话全部消息
async function onSelectConversation(id: number): Promise<void> {
  try {
    await chat.selectConversation(id)
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '加载会话消息失败')
  }
}

// 发送消息：失败时用户消息保留，仅提示
async function onSend(text: string): Promise<void> {
  try {
    await chat.sendText(text)
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '对话失败，请稍后重试')
  }
}

// 消息变化时滚动到底部
watch(
  () => chat.messages.value.length,
  async () => {
    await nextTick()
    scrollToBottom()
  }
)
watch(
  () => chat.messages.value[chat.messages.value.length - 1]?.content,
  async () => {
    await nextTick()
    scrollToBottom()
  }
)

// 滚动到底部（平滑滚动）
function scrollToBottom(): void {
  const el = scrollRef.value
  if (el) {
    el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' })
  }
}
</script>

<style scoped>
/* 页面容器：侧栏 + 会话侧栏 + 主区 */
.employee-chat-page {
  display: flex;
  height: 100vh;
  background: var(--color-bg);
}

/* 主对话区 */
.chat-main {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

/* 顶部标题栏 */
.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 24px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}

/* 移动端抽屉遮罩：展开时覆盖全屏，点击关闭 */
.drawer-mask {
  display: none;
}
@media (max-width: 767.98px) {
  .drawer-mask {
    display: block;
    position: fixed;
    inset: 0;
    z-index: 99;
    background: rgb(0 0 0 / 0.25);
  }
}

/* 移动端菜单按钮：桌面端隐藏，≤768px 显示用于展开会话抽屉 */
.mobile-menu-btn {
  display: none;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--duration-base) var(--ease-out);
}
.mobile-menu-btn:hover {
  color: var(--color-primary);
  border-color: var(--color-primary-light);
  background: var(--color-primary-lighter);
}
@media (max-width: 767.98px) {
  .mobile-menu-btn {
    display: flex;
  }
}

/* 返回按钮 */
.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--duration-base) var(--ease-out);
}
.back-btn:hover {
  color: var(--color-primary);
  border-color: var(--color-primary-light);
  background: var(--color-primary-lighter);
}

/* 标题信息列 */
.header-info {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

/* 标题 + 分类标签行 */
.header-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 标题 */
.header-title {
  font-size: var(--font-size-h2);
  line-height: var(--line-height-h2);
  font-weight: var(--font-weight-medium);
  color: var(--color-text);
}

/* 分类标签 */
.category-tag {
  padding: 1px 8px;
  background: var(--color-primary-lighter);
  color: var(--color-primary);
  font-size: var(--font-size-caption);
  line-height: var(--line-height-caption);
  border-radius: var(--radius-sm);
}

/* 简介 */
.header-intro {
  font-size: var(--font-size-caption);
  color: var(--color-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 状态角标 */
.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: var(--radius-full);
  background: var(--color-border-light);
  color: var(--color-text-secondary);
  font-size: var(--font-size-caption);
}
/* 状态圆点 */
.status-pill i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}
.status-pill.is-online i {
  background: #22c55e;
}
.status-pill.is-offline i {
  background: #9ca3af;
}

/* 消息滚动区 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

/* 空态欢迎区 */
.empty-hero {
  margin: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  max-width: 480px;
  text-align: center;
  padding: 32px;
}

/* 欢迎标题 */
.hero-title {
  font-size: 20px;
  line-height: 28px;
  font-weight: var(--font-weight-bold);
  color: var(--color-text);
}

/* 欢迎简介 */
.hero-intro {
  font-size: var(--font-size-body);
  color: var(--color-text-secondary);
}

/* 快捷问题 */
.suggestions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-top: 8px;
}

/* 建议胶囊 */
.suggestion-chip {
  padding: 8px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  background: var(--color-surface);
  color: var(--color-text-secondary);
  font-size: var(--font-size-body);
  cursor: pointer;
  transition: all var(--duration-base) var(--ease-out);
}
.suggestion-chip:hover:not(:disabled) {
  border-color: var(--color-primary-light);
  color: var(--color-primary);
  background: var(--color-primary-lighter);
  transform: translateY(-2px);
  box-shadow: var(--shadow-card-hover);
}
.suggestion-chip:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 底部输入区 */
.chat-composer-wrap {
  padding: 0 24px 24px;
}
</style>
