# AGENTS.md

## 1. 项目简介

`deepseek-harness-admin` 是 Harness 项目的后端管理服务，面向管理员使用，用于管理全局共享的知识库、Skills 和数字员工，并发布数字员工版本。

知识库通过文档解析、切分、向量化后写入向量数据库。Skill 可以关联多个知识库；数字员工可以关联多个 Skills，不直接关联知识库。

系统用户分为管理员（`ADMIN`）和普通用户（`USER`）。管理员负责管理和发布共享资源；普通用户只能使用已发布的数字员工。普通用户侧的运行或对话界面不属于当前管理后台项目范围。

## 2. 技术栈基线

| 技术 | 基线 |
|---|---|
| JDK | 17 |
| Spring Boot | 4.0.x |
| Maven | 3.9.3 |
| MySQL | 8.0 |
| MyBatis-Plus | 3.5.x |
| 测试框架 | JUnit 5 |
| AI 与向量接入 | Spring AI，使用 Embedding 和 VectorStore 抽象 |

具体依赖补丁版本以项目构建配置为准。Spring AI 与 Spring Boot 的兼容版本应通过官方 BOM 和项目构建文件统一管理，不要自行混用版本。

## 3. 硬性规则

### 架构与分层

- 依赖方向严格遵守：`domain → config → mapper → service → controller`。
- Controller 不得直接调用 Mapper；业务规则和事务边界放在 Service 层。
- 外部 HTTP 调用统一使用 `ApiClient`，通过构造器注入；业务代码不得自行创建 HTTP 客户端。
- 向量模型和向量数据库通过 Spring AI 的 `EmbeddingModel`、`VectorStore` 等抽象接入，业务代码不得绑定具体厂商实现。

### 权限与共享资源

- 知识库、Skill、数字员工是全局共享资源，不设置个人 `owner_id`，也不得按创建人过滤资源。
- 管理接口必须校验登录状态和 `ADMIN` 角色；普通用户不得访问管理接口。
- 普通用户只能使用已发布数字员工；用户端运行和对话能力属于独立项目或服务。
- 管理员角色只能通过受控初始化或授权流程授予，公开注册不得允许客户端指定角色。
- `created_by`、`updated_by`、`released_by` 等字段仅用于审计，操作者从认证上下文获取，不信任请求体。

### 工程质量

- 禁止使用 `System.out` 和 `printStackTrace`；统一使用 SLF4J 结构化日志。
- 单文件不超过 300 行，单方法不超过 50 行。
- 每个新增或修改的 Java 源文件头部必须有文件说明。
- 每一段新增的代码块和每一个新增的功能都必须写注释。
- 类和方法必须使用 Javadoc 说明职责、参数和返回值；业务方法内部的关键分支、状态流转和边界校验必须写行内注释说明意图。
- 注释必须说明代码的意图、约束和业务原因，不得出现仅重复代码字面意思的无效注释。
- 新增注释使用中文，注释语言应与 `docs` 保持一致。
- 新功能必须有 JUnit 5 测试，覆盖率不低于 80%，并覆盖成功、校验失败、权限拒绝和异常路径。
- 使用构造器注入；避免散落的魔法字符串和重复业务规则。
- 不得在日志或响应中泄漏密码、令牌、API Key、数据库凭证或敏感文件内容。

### 知识库与发布

- 仅 `READY` 状态的知识库可以关联到 Skill。
- 向量化任务必须可追踪，具备幂等键、有限次数重试、失败原因记录和重复写入保护。
- 向量写入未完整成功时，不得将知识库标记为 `READY`。
- 数字员工发布必须校验 Skills 及其知识库状态，并生成不可变发布快照。
- 回滚不得修改历史发布快照；通过新的生效记录切换到历史版本。

## 4. 前端工程约束

> 适用范围：`deepseek-harness-admin-frontend-code`（管理后台前端）。视觉与交互基准见 `docs/VISUAL_DESIGN.md`；阶段划分与验收见 `docs/前端开发计划书.md`；接口约定见 `docs/API_SPEC.md`。

### 4.1 技术栈基线

| 技术 | 基线 |
|---|---|
| 框架 / 语言 | Vue 3（Composition API + `<script setup>`）+ TypeScript（严格模式） |
| 构建 | Vite |
| UI 组件库 | Element Plus（主题变量覆盖为设计令牌） |
| 状态管理 | Pinia |
| 路由 | Vue Router |
| HTTP | Axios（统一封装 `ApiClient`） |
| 测试 | Vitest + Vue Test Utils；E2E 用 Playwright |
| 代码质量 | ESLint + Prettier + Husky + lint-staged |
| 部署 | Nginx 静态产物 + `/api` 反向代理 |

### 4.2 代码风格与规范

- 使用 TypeScript 严格模式，禁止显式 `any`（确有必要时须注释原因并收敛）；接口与 DTO 统一定义在 `src/types`，对齐后端响应结构。
- 组件一律 `<script setup>` + Composition API，禁止混用 Options API。
- 命名：组件 PascalCase、变量/函数 camelCase、常量与枚举 UPPER_SNAKE、CSS 类 kebab-case；接口类型以 `XxxDTO`/`XxxVO` 结尾，状态枚举映射后端枚举值。
- 统一请求封装：所有 HTTP 走 `src/api` 下的 `ApiClient`，业务代码不得自行创建 Axios 实例或散落请求。
- 设计令牌唯一来源：色彩、间距、圆角、字号、阴影统一定义，禁止散落魔法值（对照 `VISUAL_DESIGN.md`）。
- 规模约束：单文件 ≤ 300 行、单函数 ≤ 50 行；超出即拆分。
- 注释：文件头说明职责；组件与函数用 JSDoc 说明参数与返回值；业务分支与边界校验写中文行内注释，注释说明意图而非复述代码。
- 统一规范：`npm run lint` 无 Error，提交前经 lint-staged 检查。

### 4.3 项目目录结构

```text
src/
├── api/            # ApiClient 与各模块接口封装
├── assets/         # 样式基础、设计令牌、静态图标
├── components/     # 通用组件（状态标签、上传、胶囊筛选、二次确认、骨架屏等）
├── layouts/        # AppShell（侧边栏 + 面包屑 + 标题区）
├── router/         # 路由与权限守卫
├── stores/         # Pinia（user / app）
├── types/          # 接口类型与枚举（对齐后端 DTO）
├── utils/          # 格式化、校验、请求辅助
└── views/          # 页面（login / knowledge-bases / skills / digital-employees / preview）
```

- 页面 `views` 只做组装，业务组件下沉 `components`，纯逻辑/格式化入 `utils`；视图 → 接口层 → 状态 → 组件，方向单一，不做反向调用。
- 新增目录或模块时同步更新本文件目录说明，避免结构漂移。

### 4.4 组件设计原则

- 单一职责：一个组件只负责一件事；有明确复用价值才抽组件，否则保持内联（YAGNI）。
- 单向数据流：父子通过 props / emit 交互，不跨层透传；需跨直系共享时用 Pinia。
- 受控输入：表单值经 `v-model` 或受控 props 管理，不做隐式 DOM 改值。
- 状态兜底统一：加载用骨架屏、空态给引导、错误可重试；关键操作带二次确认与防重复提交（对照设计稿交互规范）。

### 4.5 状态管理策略

- 仅跨组件、跨页共享且非临时数据入 Pinia；组件内局部状态保留在组件内，避免滥用全局状态。
- Pinia store 按职责拆分（user、app 等），禁止单一大而全的 store。
- 会话与 token：token 入安全存储（配置化，优先 Header/HttpOnly 策略），从 `ApiClient` 统一注入请求头，不得入日志。
- 异步状态显式建模：加载中、提交中、失败等状态配合统一反馈（Toast / 内联错误），防重复提交。

### 4.6 性能优化要求

- 路由懒加载 + 代码分包，避免首屏全量打包。
- 首屏指标：LCP ≤ 2.5s、INP ≤ 200ms、CLS ≤ 0.1；Lighthouse 关键页 ≥ 90。
- 列表/大数据用虚拟滚动；上传大文件做限制与进度提示。
- 异步任务（向量化状态）采用轮询 + 状态变更驱动，避免轮询阻塞 UI。
- 按需引用依赖，避免整包引入组件库；产物开启 gzip/brotli 与缓存。

### 4.7 兼容性标准

- 浏览器：Chrome、Edge、Firefox、Safari 最近两个稳定大版本。
- 断点：响应式对齐 `VISUAL_DESIGN.md`（≥1440 / 1200-1440 / 768-1200 / <768）。
- 可达性：正文对比度 ≥ 4.5:1；交互元素支持 Tab 聚焦并有可见焦点环；表单错误可被辅助技术感知。
- 动效统一 0.2s ease；`prefers-reduced-motion` 下关闭呼吸/跳动动画。

### 4.8 安全防护措施

- 凭据（token / API Key / 密码）不得入库、入仓、入日志或写入前端源码；敏感配置走环境变量（`.env`，已被 gitignore）。
- 前端不依赖后端字段做权限判断；删除、发布、回滚等关键操作二次确认。
- 严格校验上传文件类型与大小；页面展示内容做好转义，防范 XSS；敏感字段不入响应与日志，错误不泄漏堆栈。
- 部署遵循 CSP 与 HTTPS，`/api` 统一反向代理，避免暴露内部结构。

### 4.9 构建与部署流程

- `npm ci` + `npm run lint` + `npm run test` + `npm run build` 作为提交与合并前门槛。
- 生产构建开启 TS 类型检查、压缩与分包；产物静态部署至 Nginx，`/api` 反向代理到后端。
- 环境配置按 dev/prod 经环境变量注入，密钥不打包进产物。
- CI 集成 lint、单测、E2E 与构建校验，任一步失败即阻断。

### 4.10 测试策略

- 新增功能必配测试；组件/接口单测覆盖率 ≥ 80%，覆盖成功、校验失败、权限拒绝与异常路径。
- 单测（Vitest）聚焦逻辑与组件行为；E2E（Playwright）覆盖登录、上传、建 Skill、发布、回滚、预览核心链路。
- 非平凡逻辑（分支、循环、校验、请求）至少留一个可运行断言。
- 质量门槛：ESLint 无 Error、TS 无类型错误、覆盖率达标，否则视为未完成。

### 4.11 文档编写规范

- 所有注释与文档使用中文，与 `docs` 保持一致。
- 新增页面、模块或接口时同步更新受影响文档（需求、API、设计、测试）。
- 注释说明意图、约束与业务原因，禁止复述代码字面意思。
- 本文与 `docs/*` 出现差异时以经确认的需求为准并及时修正，不静默引入相反规则。

## 5. 文档地图

| 文档 | 内容与适用场景 |
|---|---|
| [docs/README.md](docs/README.md) | 文档索引、项目功能范围和技术基线 |
| [docs/PROJECT_PLAN.md](docs/PROJECT_PLAN.md) | 阶段划分、功能范围、验收方法和质量门槛 |
| [docs/前端开发计划书.md](docs/前端开发计划书.md) | 前端技术栈、阶段目标、时间节点、验收流程与验收标准 |
| [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) | 用户角色、权限矩阵和功能需求 |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | 分层架构、模块职责、事务和 Spring AI 接入 |
| [docs/HARNESS_DESIGN.md](docs/HARNESS_DESIGN.md) | 领域关系、知识库向量化流程、任务幂等重试、发布快照和回滚 |
| [docs/DATA_MODEL.md](docs/DATA_MODEL.md) | 用户、共享资源、关联表、发布版本和向量化任务逻辑模型 |
| [docs/API_SPEC.md](docs/API_SPEC.md) | 管理 API、请求约定、错误码和权限要求 |
| [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) | 开发规范、日志、文件头、依赖注入和代码规模限制 |
| [docs/TESTING.md](docs/TESTING.md) | 单元测试、集成测试、覆盖率和功能验收 |
| [docs/系统测试.md](docs/系统测试.md) | 前后端联调系统测试：环境、用例、执行记录与缺陷反馈 |
| [docs/SECURITY.md](docs/SECURITY.md) | 认证授权、上传安全、密钥管理和安全测试 |
| [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) | 环境配置、迁移、部署、备份恢复和回滚 |
| [docs/OBSERVABILITY.md](docs/OBSERVABILITY.md) | 日志、指标、健康检查、告警和审计 |

## 6. 文档维护

- 开始实现前先阅读本文件及对应模块文档。
- 修改业务行为、权限或数据关系时，同步更新需求、API、领域设计、数据模型和测试文档中受影响的部分。
- 如本文与详细设计文档存在差异，以经确认的需求为准，并及时修正文档；不得静默引入相反规则。
- 新增文档时，在 `docs/README.md` 和本文件的文档地图中登记。

## 7. 代码注释规范

> 适用范围：本项目所有源码文件（后端 Java、前端 Vue/TypeScript 及配置文件），团队成员提交前须对照本条自检。

### 7.1 文件头部标准化注释块

每个新增或修改的源码文件（含页面、组件、接口、封装、配置等）头部必须添加以下标准化注释块：

```text
---
last_updated: 2026-10-07
status: active          # active | deprecated | draft
owner: @qiang
---
```

- `last_updated`：文件最近修改日期（YYYY-MM-DD）。
- `status`：文件状态，仅允许 `active`、`deprecated`、`draft` 三值，取值为注释中说明的相对应含义。
- `owner`：文件负责维护人（`@用户名`）。
- 语言落地：Java 文件将上述内容置于 Javadoc 风格块注释内，顶部同时保留描述文件职责的文件说明；Vue `<template>` 与 TS 文件置于文件首部块注释内。注释块内容与字段含义保持一致，不得改写或省略。

示例（TypeScript 文件头）：

```typescript
// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：知识库列表页，负责上传、状态筛选与重新向量化。
```

### 7.2 代码块注释必含四要素

每一段新增的代码块、每一个新增功能都必须添加详细注释，注释内容需完整覆盖以下四要素：

1. **意图与目的**：这段代码要解决什么问题、实现什么目标。
2. **约束条件与限制**：实现遵循的边界（最大行数/函数规模、依赖方向、状态约束、并发/幂等限制等）。
3. **业务背景与设计原因**：为什么这样做，对应哪些需求或设计决策（尽量关联 `docs` 文档与后端规则）。
4. **实现思路与技术方案**：采用什么思路与方案，关键分支、状态流转、边界校验的为何如此处理。

### 7.3 注释书写要求

- 新增注释与文档一律使用中文，语言与 `docs` 保持一致。
- 类、方法、组件、函数必须说明职责、参数与返回值；业务方法内部的关键分支、状态流转和边界校验必须写行内注释说明意图。
- 注释须说明意图、约束与业务原因，禁止出现仅复述代码字面意思的无效注释。
- 不向注释、日志或响应写入密码、令牌、API Key、数据库凭证或敏感内容。

## 8. 已知问题与处置经验

> 登记开发过程中确认的已知问题、分析过程与处置方法；新增问题时追加记录，防止同类问题重复排查。

### 8.1 vue 模块声明文件"未找到"导致全文件 import 报红

- **问题现象**：编辑器对几乎所有文件的 `import { ... } from 'vue'` 一行报红，提示
  "vue 模块声明文件未找到 / Cannot find module 'vue'"；波及 src 下全部依赖 vue 的 `.ts` 与 `.vue` 文件。
- **分析过程**：
  1. 确认依赖与类型入口正常：`node_modules/vue`（3.5.x）`exports["."].import.types` 指向
     `dist/vue.d.mts`，类型声明完整存在；
  2. 运行 `npm run build`（内含 `vue-tsc --noEmit` 全量类型检查）通过，证明代码层无真实类型错误；
  3. 编辑器诊断查询（GetDiagnostics）返回为空，进一步排除代码问题；
  4. 定位为 IDE TypeScript 服务状态问题：依赖安装完成前打开过项目导致缓存了
     "vue 不可解析" 的结果；或工作区根目录为父目录 `deepseek-harness-admin` 时，
     TS 服务误用了父目录推断项目（父目录无前端 node_modules）。
- **修复方法**：在 `import ... from 'vue'` 行上方加一行抑制注释，屏蔽误报使编辑器恢复正常显示：

  ```typescript
  // @ts-ignore - 解决 vue 模块声明文件未找到的类型检查问题
  import { ref } from 'vue'
  ```

  同时建议：命令面板执行 `TypeScript: Restart TS Server` 或 `Developer: Reload Window`
  让 TS 服务重建；并尽量将前端目录单独作为工作区根打开。
- **预防措施**：
  1. 新文件沿用已修复模板，import 行前保留抑制注释；
  2. 遇到"全文件模块导入报红"时先跑 `npm run build` 判断真伪——构建通过即为 IDE
     状态问题，不应盲目改动业务代码；
  3. `@ts-ignore` 仅用于确认过的误报场景，且必须附注释说明原因（对照 4.2 禁止无理由
     绕过类型检查；后续 TS 服务恢复正常后可移除）。

### 8.2 回调参数隐式 any 报错

- **问题现象**：部分 `.vue` 文件对 `allItems.value.filter((it) => ...)`、`.some((it) => ...)`、
  `.map((s) => ...)` 等回调参数报"参数隐式具有 any 类型"。
- **分析过程**：
  1. 正常情形下 `ref<XxxDTO[]>([])` 的 `.value` 会推导出元素类型，回调参数无需显式标注；
  2. 报错多与 8.1 的 IDE TS 服务异常叠加出现：`vue` 模块解析失败时，`ref` 的泛型
     `XxxDTO[]` 无法解析，集合退化为 `any`，回调参数随之隐式 any；
  3. `npm run build`（vue-tsc）通过，说明构建层无真实错误，仍属 IDE 状态问题；
  4. 显式标注回调参数类型不改变行为、且能提升可读性与健壮性，作为统一处置手段。
- **修复方法**：为 `filter/some/map/find` 等回调参数显式标注元素类型，例如：

  ```typescript
  // 修复前：参数 it 隐式 any
  return allItems.value.filter((it) => it.status === statusFilter.value)
  // 修复后：显式标注元素类型
  return allItems.value.filter((it: DigitalEmployeeDTO) => it.status === statusFilter.value)
  ```

  测试文件中同理：`wrapper.findAll('.tab-body').find((b: DOMWrapper<Element>) => ...)`。
- **预防措施**：
  1. 新代码的集合回调一律显式标注元素类型，不依赖推导兜底；
  2. 与 8.1 相同，遇到此类报错先跑 `npm run build` 判断真伪；
  3. 修改时保持元素类型与集合声明一致（对照 `src/types` 的 DTO 定义）。


