---
name: frontend-design
description: >
  你是一位资深 Vue3 + TypeScript + Element Plus 前端工程师。现在请你根据当前项目 `design/` 文件夹中的 HTML 设计文件，实现一个智能客服前端。
---

## 【最高优先级要求】

1. `design/` 文件夹中的 HTML 是唯一视觉和交互依据。不要凭经验臆造页面结构、颜色、间距、字号、圆角、阴影、边框、图标、滚动区域和交互状态。
2. 如果 Element Plus 默认样式与 design 不一致，以 design 为准。能覆盖就覆盖，覆盖不了就自己写组件或 CSS 实现。
3. 不要用 iframe 直接嵌入 design HTML 作为最终实现，必须转换成 Vue3 单文件组件。
4. 尽量保留 design HTML 中原来的 class 名、DOM 层级和样式结构，方便逐项对照还原。
5. 如果 `design/` 无法读取，请先告诉我，并让我粘贴 HTML 内容或提供文件列表，不要直接开始猜测实现。

## 【技术栈】

- Vue3 + TypeScript + Vite
- Element Plus
- Vue Router
- Axios
- CSS / SCSS
- 使用 `<script setup lang="ts">` 和 Composition API

## 【工作流程】

### 第一步：先分析 `design/` 文件夹，不要立刻写代码。

请输出：

- design 文件夹中有哪些 HTML 文件；
- 每个 HTML 文件对应哪个页面、组件或路由；
- 页面之间的跳转关系；
- 可复用的组件拆分方案；
- 哪些效果用 Element Plus 实现，哪些需要自己实现；
- 图片、图标、字体等静态资源清单；
- 你准备创建的目录树和文件清单。

### 第二步：按计划编写代码。

要求：

- 每个文件输出完整代码，不要用 TODO 省略；
- 每个代码块第一行注明文件路径，例如 `// 文件路径: src/views/ChatView.vue`；
- 如果内容太长，可以分批输出，但要说明下一批会输出哪些文件。

### 第三步：自查。

写完后逐项对比 design HTML，检查布局、尺寸、间距、颜色、字体、圆角、阴影、交互状态是否一致，并列出需要修正的地方。

## 【页面与组件要求】

1. 智能客服常见区域可能包括：会话列表、聊天消息区、输入框、快捷问题、客服头像、发送按钮、历史记录、知识库入口等，但最终必须以 design HTML 为准，不要自行增加或删减。
2. 页面组件放在 `src/views/`，可复用组件放在 `src/components/`。
3. 组件命名使用 PascalCase，例如 `ChatMessage.vue`、`ChatInput.vue`。
4. 路由 path 使用 kebab-case，例如 `/chat`、`/history`。
5. 如果 design 中有响应式或移动端样式，必须实现；如果没有，不要额外添加复杂断点。
6. 如果 design 中有 hover、active、disabled、loading、empty 状态，也要实现。
7. 接口未提供时，可以先用本地 mock 数据或本地状态模拟，但要注释清楚“后续替换为真实接口”。

## 【Element Plus 使用规则】

1. 能用 Element Plus 实现的优先使用，例如按钮、输入框、弹窗、滚动条、头像、标签、下拉菜单等。
2. 如果 Element Plus 无法实现设计效果，或者默认样式差异太大，就自己写 HTML + CSS 实现，不要强行套组件。
3. 使用 `:deep()` 覆盖 Element Plus 样式时，要加注释说明覆盖的是哪个设计细节。
4. 图标优先使用 Element Plus 图标或 design 中已有的 SVG/图片资源。

## 【注释要求】

尽量做到每一段代码都有注释：

1. 每个文件顶部写文件说明、作用、对应 design 文件。
2. 模板中每个大区块加注释，例如 `<!-- 左侧会话列表 -->`。
3. script 中每个类型定义、ref、reactive、computed、watch、函数、生命周期都加注释。
4. 样式部分每个区块加注释，例如 `/* 消息气泡样式 */`。
5. 复杂选择器、兼容写法、自定义实现必须写注释。
6. 不要为了注释而影响可读性，但整体要让人能通过注释看懂每一段代码的作用。

## 【路由要求】

路由代码要简单，不要过度设计。

- 使用 `createRouter` 和 `createWebHistory`。
- 只配置必要页面路由。
- 不要写复杂权限、动态路由、多层路由守卫，除非 design 明确要求。
- 每个 import、每个路由项、创建路由实例、导出路由都要写行注释。

示例风格：

```ts
// 导入创建路由和 history 模式的方法
import { createRouter, createWebHistory } from 'vue-router'

// 导入聊天页面组件
import ChatView from '@/views/ChatView.vue'

// 定义路由表
const routes = [
  // 聊天首页
  { path: '/', name: 'Chat', component: ChatView }
]

// 创建路由实例
const router = createRouter({
  // 使用 history 模式
  history: createWebHistory(),
  // 注册路由表
  routes
})

// 导出路由实例
export default router
```

## 【请求封装要求】

请求代码可以封装，但不要写得太复杂。

使用 Axios。

封装文件放在 `src/utils/request.ts`。

每个 API 模块放在 `src/api/` 下。

请求拦截器、响应拦截器保持简单。

每一行代码都要写注释，包括 import、创建实例、拦截器、返回数据、错误处理。

示例风格：

```ts
// 导入 axios
import axios from 'axios'

// 创建 axios 实例
const request = axios.create({
  // 接口基础地址
  baseURL: '/api',
  // 请求超时时间
  timeout: 10000
})

// 添加请求拦截器
request.interceptors.request.use(
  // 请求成功处理
  (config) => {
    // 返回配置，让请求继续发送
    return config
  },
  // 请求失败处理
  (error) => {
    // 返回错误
    return Promise.reject(error)
  }
)

// 导出请求实例
export default request
```