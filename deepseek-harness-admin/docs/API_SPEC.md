# API 约定

接口前缀统一为 `/api`（如 `/api/auth/login`）。**实现约定**：业务成功与失败均以 HTTP 200 返回，业务结果由响应体 `code` 区分；每个响应携带 `traceId`，响应头同时回写 `X-Trace-Id`。

## 1. 统一响应格式

```json
{
  "code": "SUCCESS",
  "message": "success",
  "data": {},
  "traceId": "trace-id"
}
```

失败响应也必须使用统一结构。`data` 无内容时返回 `null` 或空对象，保持项目内一致。

## 2. 用户接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/register` | 按服务端策略创建普通用户账号；不得由请求体指定角色（服务端固定为 `USER`） |
| POST | `/api/auth/login` | 登录，成功返回 `Bearer` 凭证与用户信息 |
| POST | `/api/auth/logout` | 退出登录，吊销当前凭证，重复退出幂等 |

管理后台的管理 API 均要求已认证的 `ADMIN` 角色。普通用户只能使用已发布数字员工的运行能力，该能力由独立用户端或运行时服务提供，不属于本管理 API。

## 3. 知识库接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/knowledge-bases` | 上传知识库（`multipart/form-data`） |
| GET | `/api/knowledge-bases` | 查看知识库（分页 `page`/`size`） |
| GET | `/api/knowledge-bases/{id}` | 查看知识库详情 |
| POST | `/api/knowledge-bases/{id}/reprocess` | 重新处理知识库（清理旧向量后重新向量化） |

上传接口使用 `multipart/form-data`（字段 `file`，可选字段 `name`），服务端校验文件类型（默认 `txt/md/pdf/docx`）、大小（默认 20MB）与名称。

上传成功后返回知识库 ID 和处理状态。**上传会自动触发异步向量化**；向量化成功后进入 `READY`，解析失败（如暂不支持的类型）经有限重试后进入 `FAILED` 并记录失败原因。客户端通过详情接口查询 `UPLOADING`、`PROCESSING`、`READY` 或 `FAILED` 状态。

## 4. Skill 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/skills` | 添加 Skill |
| GET | `/api/skills` | 查看 Skills（分页） |
| GET | `/api/skills/{id}` | 查看 Skill 详情 |

创建 Skill 时请求体需要包含一个或多个 `knowledgeBaseIds`。知识库为全局共享资源且必须处于 `READY` 状态，否则返回 `KNOWLEDGE_BASE_NOT_READY`。该接口仅管理员可调用。

## 5. 数字员工接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/digital-employees` | 添加数字员工 |
| GET | `/api/digital-employees` | 查看数字员工（分页） |
| GET | `/api/digital-employees/{id}` | 查看详情 |
| POST | `/api/digital-employees/{id}/publish` | 发布数字员工 |
| POST | `/api/digital-employees/{id}/rollback` | 回滚到指定发布记录 |
| POST | `/api/digital-employees/{id}/chat` | 管理端预览对话（发起/延续会话并一次性返回回复） |
| GET | `/api/digital-employees/{id}/conversations` | 会话历史（分页） |
| GET | `/api/digital-employees/{id}/conversations/{conversationId}/messages` | 查询指定会话的消息 |

创建数字员工时请求体需要包含一个或多个 `skillIds`，不接收 `knowledgeBaseIds`（多余字段由反序列化忽略）。数字员工可使用的知识库由所选 Skills 间接确定。该接口仅管理员可调用。

发布接口校验名称、提示词、模型配置、Skills 及其展开后的知识库状态（均需可用/`READY`），通过后生成不可变配置快照与递增版本号；校验失败返回 `DIGITAL_EMPLOYEE_NOT_PUBLISHABLE`，并保留失败原因发布记录。

回滚接口请求体为 `{"releaseId": 123}`，只能选择本数字员工且为 `SUCCESS` 的发布记录；回滚前校验历史快照引用的资源仍可用，成功后基于所选快照生成新的生效记录，不修改历史快照。

**管理端预览对话接口**：

- `POST /api/digital-employees/{id}/chat`
  - 用途：管理员在数字员工预览页与数字员工对话，发布前/后验证回复效果；同时承载「对话发起」「消息发送」「消息接收」（对照 [REQUIREMENTS.md](./REQUIREMENTS.md) 3.8）。
  - 请求体：`{ "message": "用户提问内容", "conversationId": 123 }`，`message` 为必填、非空白、长度 ≤4000；`conversationId` 可空，为空表示发起新会话。
  - 响应：`ApiResponse` 的 `data` 为 `{ "conversationId": 123, "reply": "完整回复", "createdAt": "..." }`，一次性返回完整回复（非流式）。
  - 逻辑：基于数字员工当前配置（系统提示词、模型配置及关联 Skills 展开的 READY 知识库）检索相关片段、组装上下文并调用聊天模型生成回答；将用户问题与回复持久化到 `preview_conversation`/`preview_message`。
- `GET /api/digital-employees/{id}/conversations`：分页查询会话历史（返回会话 ID、标题、消息数与最近消息摘要）。
- `GET /api/digital-employees/{id}/conversations/{conversationId}/messages`：查询指定会话的消息列表（正序）。

约定：

- 鉴权：置于 `/api/digital-employees` 下且要求 `ADMIN` 角色；普通用户调用返回 `RESOURCE_FORBIDDEN`，未登录返回 `AUTH_UNAUTHORIZED`。
- 模型接入：通过 Spring AI `ChatModel`（OpenAI 兼容 provider，环境变量配置），对话模型未配置或调用失败时分别返回 `CHAT_PROVIDER_NOT_CONFIGURED`、`CHAT_FAILED`。
- 持久化：会话与消息落库；对话内容不进入审计日志。
- 该能力仅用于管理验证，不替代普通用户端的运行/对话服务。

示例（chat 请求体）：

```json
{
  "name": "售后助手",
  "description": "处理售后咨询",
  "systemPrompt": "你是售后服务助手",
  "skillIds": [101, 102],
  "modelConfig": {
    "model": "configured-model"
  }
}
```

## 6. 接口要求

- 受保护接口必须校验登录状态和所需角色。
- 知识库、Skill、数字员工及发布管理接口必须校验 `ADMIN` 角色，由认证拦截器 + `@RequireAdmin` 统一实施。
- 全局共享资源不按创建用户过滤；关联时校验资源存在及状态。
- 管理接口不得信任客户端传入的角色或审计用户 ID，操作者从认证上下文获取。
- 列表接口支持分页，分页参数统一为 `page`/`size`（单页上限 100）。
- 请求参数使用校验注解进行基础校验。
- Controller 只负责参数接收、鉴权入口和响应转换。
- 业务异常由统一异常处理器转换为统一错误响应，不向客户端暴露堆栈与内部连接信息。

## 7. 错误码

| 错误码 | 含义 |
|---|---|
| AUTH_INVALID_CREDENTIALS | 登录凭证错误（密码错误/账号不存在） |
| AUTH_UNAUTHORIZED | 未登录或凭证无效 |
| USERNAME_EXISTS | 账号已存在 |
| ACCOUNT_DISABLED | 账号已禁用 |
| RESOURCE_NOT_FOUND | 资源不存在 |
| RESOURCE_FORBIDDEN | 无资源访问权限（普通用户访问管理接口） |
| VALIDATION_FAILED | 参数校验失败（含文件类型/大小、Skill 不可用） |
| KNOWLEDGE_BASE_PROCESSING | 知识库正在向量化处理 |
| KNOWLEDGE_BASE_NOT_READY | 知识库尚未准备完成 |
| VECTOR_INDEX_FAILED | 向量化或向量数据库写入失败 |
| DIGITAL_EMPLOYEE_NOT_PUBLISHABLE | 数字员工不满足发布/回滚条件 |
| CHAT_PROVIDER_NOT_CONFIGURED | 对话模型未配置（环境变量） |
| CHAT_FAILED | 对话生成失败 |
| INTERNAL_ERROR | 系统内部错误 |
