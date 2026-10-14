# 数据模型

以下为逻辑模型，实际字段类型、命名和审计字段以数据库设计及项目代码为准。

## 1. 用户表 `user`

| 字段 | 说明 |
|---|---|
| id | 用户主键 |
| username | 登录账号，唯一 |
| password_hash | 密码哈希 |
| role | 用户角色：`ADMIN` 或 `USER` |
| status | 用户状态 |
| created_at | 创建时间 |
| updated_at | 更新时间 |

管理员角色只能由受控初始化或授权流程授予，不能由公开注册请求指定。

## 1.1 认证凭证表 `user_auth_token`

| 字段 | 说明 |
|---|---|
| id | 凭证主键 |
| user_id | 用户 ID |
| token_hash | 凭证哈希（SHA-256），唯一 |
| expires_at | 过期时间 |
| revoked | 是否已吊销 |
| created_at | 创建时间 |

登录签发凭证时数据库只保存哈希，原始凭证仅返回一次；退出登录标记吊销（幂等）；登录时顺带清理该用户的过期凭证。

## 2. 知识库表 `knowledge_base`

| 字段 | 说明 |
|---|---|
| id | 知识库主键 |
| name | 知识库名称 |
| status | 上传处理状态 |
| file_name | 原始文件名 |
| storage_key | 文件存储标识 |
| chunk_count | 文档片段数量 |
| vector_store | 向量数据库标识 |
| embedding_model | 向量模型标识 |
| failure_reason | 失败原因 |
| created_by | 创建管理员 ID，仅用于审计 |
| created_at | 创建时间 |
| updated_at | 更新时间 |

## 3. Skill 表 `skill`

| 字段 | 说明 |
|---|---|
| id | Skill 主键 |
| name | Skill 名称 |
| description | Skill 描述 |
| content | Skill 配置或实现内容 |
| status | Skill 状态 |
| version | Skill 版本 |
| created_by | 创建管理员 ID，仅用于审计 |
| created_at | 创建时间 |
| updated_at | 更新时间 |

## 4. 数字员工表 `digital_employee`

| 字段 | 说明 |
|---|---|
| id | 数字员工主键 |
| name | 名称 |
| description | 描述 |
| system_prompt | 系统提示词 |
| model_config | 模型配置 |
| status | 草稿、已发布或归档 |
| published_version | 当前发布版本 |
| created_by | 创建管理员 ID，仅用于审计 |
| created_at | 创建时间 |
| updated_at | 更新时间 |

## 5. 关联表

- `skill_knowledge_base`
  - `skill_id`
  - `knowledge_base_id`
- `digital_employee_skill`
  - `digital_employee_id`
  - `skill_id`

数字员工不直接关联知识库。其实际知识库集合通过数字员工关联的 Skills 计算得到。

关联表应设置联合唯一约束（实现中为 `(skill_id, knowledge_base_id)` 与 `(digital_employee_id, skill_id)`），避免同一资源被重复关联；Service 层在写入前还会对请求中的 ID 去重。

## 6. 发布版本表 `digital_employee_release`

| 字段 | 说明 |
|---|---|
| id | 发布记录主键 |
| digital_employee_id | 数字员工 |
| version | 发布版本号 |
| config_snapshot | 配置快照 |
| released_by | 发布人 |
| released_at | 发布时间 |
| status | 发布状态 |
| failure_reason | 失败原因 |

## 7. 知识库处理任务表 `knowledge_base_task`

| 字段 | 说明 |
|---|---|
| id | 任务主键 |
| knowledge_base_id | 知识库 ID |
| file_version | 原始文件版本 |
| idempotency_key | 幂等键，唯一 |
| status | 任务状态 |
| retry_count | 已重试次数 |
| failure_reason | 最后失败原因 |
| started_at | 开始时间 |
| finished_at | 完成时间 |
| created_at | 创建时间 |

同一知识库、文件版本和处理配置只能存在一个有效幂等任务。

## 8. 向量数据

向量数据库不建议作为 MySQL 业务表直接存储。向量数据库中至少应保存：

- `knowledge_base_id`
- 文档片段 ID
- 文档片段内容或可定位引用
- 向量值
- 文件、页码、段落等元数据
- 向量模型标识和版本

MySQL 保存知识库处理状态、向量数据库标识、模型标识和任务结果，向量数据库保存实际向量和检索元数据。

**实现说明**：生产默认使用 Redis 向量库（RediSearch，`spring.ai.vectorstore.type=redis`），开发可切换内存 `SimpleVectorStore`（`local`）；向量片段写入的元数据键为 `knowledgeBaseId` 与 `fileVersion`；片段 ID 为 `kb-{知识库ID}-{存储键}-{序号}`，支撑幂等 upsert 与按知识库过滤删除。向量索引维度取自当前 Embedding 模型。

## 8.1 预览对话（持久化）

- 管理端预览对话为**持久化**能力，新增 `preview_conversation` 与会话 `preview_message`（对照 [HARNESS_DESIGN.md](./HARNESS_DESIGN.md) 第 10 章）。
- `preview_conversation`：`id`、`digital_employee_id`、`title`、`created_by`（仅审计）、`created_at`、`updated_at`；按数字员工建立索引。
- `preview_message`：`id`、`conversation_id`、`role`（`USER`/`ASSISTANT`）、`content`、`created_at`；按会话建立索引。
- 检索复用现有 `knowledge_base` 的向量数据与关联关系；数字员工当前配置（系统提示词、模型配置、Skills 展开的知识库）来自 `digital_employee`/`skill`/`skill_knowledge_base` 现有数据，不新增存储。
- 会话与消息用于管理验证的历史回看；对话内容**不进入审计日志**。

## 9. 数据约束

- 用户名唯一。
- 用户角色只能由受控授权流程修改。
- 知识库、Skill、数字员工均为全局共享资源，不设置 `owner_id`。
- 管理操作校验管理员角色；关联资源时校验资源存在及状态，不校验个人所有权。
- `skill_knowledge_base` 只能关联状态为 `READY` 的知识库。
- `digital_employee_skill` 中的 Skill 必须处于可用状态。
- `user_auth_token.token_hash` 唯一，`expires_at` 与 `revoked` 共同决定凭证有效性。
- 删除或重新处理知识库前，需要检查是否被 Skill 或已发布版本引用。
- `created_by`、`updated_by`、`released_by` 等用户标识仅用于审计，不作为资源所有权。
- `knowledge_base_task.idempotency_key` 必须唯一。
- 发布版本记录必须不可变；回滚只能创建新的生效记录。
- 时间字段统一使用项目约定的时区和格式。
- 密码、令牌、第三方密钥等敏感信息不得明文记录日志。
