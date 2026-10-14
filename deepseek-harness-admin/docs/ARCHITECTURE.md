# 系统架构

## 1. 架构目标

系统采用分层架构，保持领域模型、基础设施、数据访问、业务服务和接口层职责清晰。

## 2. 依赖方向

依赖方向必须保持如下顺序：

```text
domain → config → mapper → service → controller
```

说明：

- `domain`：领域对象、枚举、请求响应模型和业务常量。
- `config`：数据库、认证、序列化、HTTP 客户端等基础配置。
- `mapper`：MyBatis-Plus Mapper、持久化对象和数据访问实现。
- `service`：业务编排、领域规则和事务边界。
- `controller`：HTTP 参数接收、鉴权入口、响应转换。

上层可以调用下层，下层不得反向依赖上层。Controller 不应直接访问 Mapper。

## 3. 推荐模块

```text
src/main/java/com/harness/admin
├── domain
│   ├── common            # 统一响应、错误码、业务异常、分页结果
│   ├── user              # 用户角色/状态、注册/登录 DTO、用户主体与认证上下文
│   ├── knowledgebase     # 知识库状态、任务状态、响应模型
│   ├── skill             # Skill 状态、请求/响应模型
│   ├── digitalemployee   # 数字员工状态、请求/响应、发布状态、回滚请求
│   └── previewchat       # 预览对话请求/响应、对话上下文与检索结果模型
├── config
│   ├── TraceIdFilter / AuthProperties / StorageProperties / VectorizationProperties
│   ├── AuthInterceptor / RequireAdmin / WebSecurityConfig / SecurityBeansConfig
│   ├── SpringAiConfig / LocalHashEmbeddingModel / FileStorageService
│   ├── MybatisPlusConfig / TaskConfig / GlobalExceptionHandler / AdminInitializer
│   └── StorageHealthIndicator
├── mapper
│   ├── entity            # User、UserAuthToken、KnowledgeBase、KnowledgeBaseTask、Skill、DigitalEmployee、DigitalEmployeeRelease 及关联实体
│   └── UserMapper、KnowledgeBaseMapper、SkillMapper、DigitalEmployeeMapper 等
├── service
│   ├── AuthService / LoginAttemptGuard
│   ├── KnowledgeBaseService / DocumentParser / TextChunkSplitter / VectorIndexService
│   ├── VectorizationTaskService / VectorizationTaskStateService
│   ├── SkillService / DigitalEmployeeService
│   ├── DigitalEmployeeReleaseService / ReleaseSnapshotService
│   └── PreviewChatService   # 管理端预览对话：检索 + 上下文组装 + 一次性生成
└── controller
    ├── HealthController / AuthController
    ├── KnowledgeBaseController / SkillController / DigitalEmployeeController
    └── PreviewChatController
```

具体包名以工程实际为准（如上）。依赖方向 `domain → config → mapper → service → controller` 不变；认证拦截器等 Web 安全基础设施归入 `config`，仅依赖 Service 层接口。

## 4. 业务模块

### 用户模块

负责账号注册或受控初始化、登录、退出登录、身份凭证校验、`ADMIN`/`USER` 角色授权和用户状态。公开注册不得授予管理员角色。

### 知识库模块

负责文件上传、文件元数据、文档解析、文本切分、向量化、向量数据库写入、知识库状态和知识库查询。

### Skill 模块

负责 Skill 创建、内容校验、列表查询以及与多个知识库的关联管理。

### 数字员工模块

负责数字员工创建、配置查询、与多个 Skills 的关联和发布。数字员工不直接关联知识库。

### 预览对话模块

负责管理端预览页的实时对话：加载数字员工当前配置、展开其关联 READY 知识库、复用 `EmbeddingModel`/`VectorStore` 检索相关片段、组装上下文并调用聊天模型一次性生成回答；会话与消息持久化，接口须校验 `ADMIN` 角色。

### 发布模块

负责发布前校验、版本生成、发布记录和发布状态。

## 5. 事务边界

- 注册：用户创建和唯一性校验应处于同一业务事务。
- 上传知识库：元数据创建与处理任务状态更新应可追踪；向量数据库写入失败时不得标记为 `READY`。
- 创建 Skill：Skill 主体和知识库关联应保证一致性。
- 创建数字员工：主体信息与 Skill 关联应保证一致性。
- 发布数字员工：发布校验、版本创建和发布记录应处于同一业务事务。
- 管理接口由认证与角色授权层保护；共享资源不按创建用户隔离。

## 6. 外部调用

所有 HTTP 调用必须通过统一的 `ApiClient`，使用构造器注入。业务服务不得直接创建 HTTP 客户端或散落调用第三方 HTTP API。

## 7. Spring AI 与向量能力

- 使用 Spring AI 的 Embedding 接口连接向量模型。
- 使用 Spring AI 的 `VectorStore` 抽象连接向量数据库。
- 具体向量模型和向量数据库由配置决定，业务服务不直接依赖具体厂商实现。
- 文件解析、切分、Embedding 和向量写入建议作为异步处理任务执行。
- 处理任务必须支持状态查询、失败原因记录和有限次数重试。
- 向量模型变更时应记录模型名称和版本，避免不同模型生成的向量混用。

**实现说明**：缺省由 `SpringAiConfig` 提供本地哈希向量模型（`LocalHashEmbeddingModel`，基于 SHA-256 生成确定性向量）与内存向量库（`SimpleVectorStore`）；通过 `@ConditionalOnMissingBean` 确保引入真实厂商 starter（如 OpenAI）后自动替换，业务代码（`VectorIndexService`、`VectorizationTaskService`）只面向 `EmbeddingModel`/`VectorStore` 抽象。模型与向量库标识记录在知识库表（`embedding_model`/`vector_store`），缺省为 `local-hash`/`simple-vector-store`，可通过 `harness.vectorization.*` 配置覆盖。

## 8. 管理后台边界

- 当前服务面向管理员，提供知识库、Skill、数字员工、发布版本与**管理端预览对话**的管理能力。
- **管理端预览对话**为管理员在预览页的实时对话验证（基于数字员工当前配置检索生成），用于管理验证，不替代普通用户侧的运行/对话服务。
- 普通用户仅能使用已发布数字员工；运行、对话和终端用户体验由独立用户端或运行时服务负责。
- `created_by`、`updated_by`、`released_by` 用于审计，不作为资源所有权或读取过滤条件。
- 管理 API 根据角色授权，普通用户调用管理接口应返回禁止访问。

## 9. 预览对话处理流程

1. 管理员在预览页输入问题，前端调用 `POST /api/digital-employees/{id}/chat`。
2. `PreviewChatService` 解析或创建会话（校验归属），并加载数字员工当前配置，展开其关联 Skills 下的 READY 知识库集合。
3. 复用 `EmbeddingModel` 对用户问题向量化，经 `VectorStore` 检索相关文档片段。
4. 组装 `系统提示词 + 文档片段 + 用户问题` 上下文，调用聊天模型。
5. 一次性返回完整回复，并将用户问题与回复写入 `preview_message`；会话记录于 `preview_conversation`。

检索与生成均面向 Spring AI 抽象（`EmbeddingModel`/`VectorStore`/`ChatModel`），不绑定具体厂商；对话内容不进入审计日志。
