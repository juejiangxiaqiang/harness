<!-- 文件路径: design/src/views/ChatView.vue -->
<!-- 作用: deepseek harness 对话页 —— 保留原页面的会话侧栏/消息流/输入区/历史/设置布局结构，
     仅替换视觉风格为统一设计体系；对话功能基于本地模拟逻辑，接入真实模型即可切换 -->
<template>
  <div class="chat-page">
    <!-- 移动端抽屉展开时的点击遮罩 -->
    <div v-if="sidebarOpen" class="drawer-mask" @click="sidebarOpen = false" />

    <!-- 左侧会话侧栏（历史记录/新建会话/设置入口） -->
    <ChatSessionSidebar :chat="chat" :open="sidebarOpen" @open-settings="settingsOpen = true" />

    <!-- 主对话区 -->
    <main class="chat-main">
      <!-- 顶部标题栏 -->
      <header class="chat-header">
        <!-- 移动端：展开会话侧栏抽屉 -->
        <button class="mobile-menu-btn" title="会话列表" @click="sidebarOpen = !sidebarOpen">
          <el-icon :size="18"><Menu /></el-icon>
        </button>
        <!-- 助手头像 -->
        <EmployeeAvatar char="深" color="#2563eb" :size="40" />
        <div class="header-info">
          <h2 class="header-title">DeepSeek Harness</h2>
          <p class="header-intro">{{ persona.intro }}</p>
        </div>
        <!-- 就绪状态 -->
        <span class="status-pill"><i />就绪</span>
        <!-- 设置入口 -->
        <button class="header-settings" title="对话设置" @click="settingsOpen = true">
          <el-icon :size="18"><Setting /></el-icon>
        </button>
      </header>

      <!-- 消息滚动区 -->
      <div ref="scrollRef" class="chat-messages">
        <!-- 空态欢迎区 -->
        <div v-if="messages.length === 0" class="empty-hero">
          <EmployeeAvatar char="深" color="#2563eb" :size="56" />
          <h3 class="hero-title">你好，我是 DeepSeek Harness</h3>
          <p class="hero-intro">{{ persona.intro }}。试试下面这些快捷任务：</p>
          <!-- 快捷建议 -->
          <div class="suggestions">
            <button
              v-for="s in suggestions"
              :key="s"
              class="suggestion-chip"
              @click="chat.sendText(s)"
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
            avatar-char="深"
            avatar-color="#2563eb"
          />
        </template>
      </div>

      <!-- 底部输入区 -->
      <div class="chat-composer-wrap">
        <ChatComposer
          :streaming="chat.isStreaming.value"
          :enter-to-send="chat.settings.value.enterToSend"
          @send="chat.sendText"
          @stop="chat.stopStreaming()"
        />
      </div>
    </main>

    <!-- 设置抽屉 -->
    <SettingsDrawer v-model="settingsOpen" :settings="chat.settings.value" @update:settings="chat.updateSettings" />
  </div>
</template>

<script setup lang="ts">
// harness 对话页逻辑
import { onMounted, ref, watch, nextTick } from 'vue'
import { Menu, Setting } from '@element-plus/icons-vue'
import ChatSessionSidebar from '@/components/ChatSessionSidebar.vue'
import MessageItem from '@/components/MessageItem.vue'
import ChatComposer from '@/components/ChatComposer.vue'
import SettingsDrawer from '@/components/SettingsDrawer.vue'
import EmployeeAvatar from '@/components/EmployeeAvatar.vue'
import { useChat } from '@/composables/useChat'
import { harnessPersona } from '@/mock/chat'

// 对话核心逻辑（会话存储作用域为 harness）
const chat = useChat('harness', harnessPersona)
// 当前会话消息（模板使用）
const messages = chat.messages

// 助手人设信息
const persona = harnessPersona

// 设置抽屉可见性
const settingsOpen = ref(false)
// 移动端会话侧栏抽屉可见性
const sidebarOpen = ref(false)

// 消息滚动容器引用
const scrollRef = ref<HTMLElement | null>(null)

// 快捷任务建议（空态展示）
const suggestions = [
  '帮我写一个 Vue 3 组件',
  '分析当前项目结构',
  '制定一个发布计划',
  '检查代码中的潜在问题'
]

// 页面挂载时确保存在可用会话
onMounted(() => {
  chat.ensureSession()
})

// 移动端：切换会话后自动收起侧栏抽屉
watch(
  () => chat.activeId.value,
  () => {
    sidebarOpen.value = false
  }
)

// 消息新增或内容更新时滚动到底部
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
/* 页面容器：侧栏 + 主区横向布局，占满全屏 */
.chat-page {
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

/* 顶部标题栏：白底分隔线 */
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

/* 标题信息列 */
.header-info {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

/* 标题 */
.header-title {
  font-size: var(--font-size-h2);
  line-height: var(--line-height-h2);
  font-weight: var(--font-weight-medium);
  color: var(--color-text);
}

/* 简介 */
.header-intro {
  font-size: var(--font-size-caption);
  line-height: var(--line-height-caption);
  color: var(--color-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 就绪状态胶囊 */
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
  background: #22c55e;
}

/* 设置按钮 */
.header-settings {
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
.header-settings:hover {
  color: var(--color-primary);
  border-color: var(--color-primary-light);
  background: var(--color-primary-lighter);
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

/* 快捷建议 */
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
.suggestion-chip:hover {
  border-color: var(--color-primary-light);
  color: var(--color-primary);
  background: var(--color-primary-lighter);
  transform: translateY(-2px);
  box-shadow: var(--shadow-card-hover);
}

/* 底部输入区 */
.chat-composer-wrap {
  padding: 0 24px 24px;
}
</style>
