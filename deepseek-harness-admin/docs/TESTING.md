# 测试与验收

## 1. 总体要求

- 测试框架使用 JUnit 5。
- 每个新功能必须新增对应测试。
- 测试覆盖率目标不低于 80%。
- 测试必须覆盖成功、参数错误、无权限和异常路径。

**实现状态**：后端当前共 **90 项测试**（单元 57 + 集成 33）全部通过，JaCoCo 指令覆盖率 **92%**（门槛 80%）。测试清单：

- 单元测试：`ApiResponseTest`、`AuthServiceTest`、`KnowledgeBaseServiceTest`、`VectorizationTaskServiceTest`、`TextChunkSplitterTest`、`VectorIndexServiceTest`、`SkillServiceTest`、`DigitalEmployeeServiceTest`、`DigitalEmployeeReleaseServiceTest`
- 集成测试：`HealthControllerIntegrationTest`、`AuthControllerIntegrationTest`、`KnowledgeBaseControllerIntegrationTest`、`VectorizationFlowIntegrationTest`、`SkillControllerIntegrationTest`、`DigitalEmployeeControllerIntegrationTest`、`DigitalEmployeeReleaseIntegrationTest`

集成测试使用内存 H2（MySQL 模式）+ Flyway 迁移，不依赖外部 MySQL；向量化使用本地哈希向量模型与内存向量库，不依赖真实厂商服务。覆盖率报告在 `mvn verify` 后生成于 `target/site/jacoco`。

## 2. 单元测试

重点测试 Service 层：

- 注册时账号唯一性校验。
- 普通用户不能通过注册请求设置管理员角色。
- 管理员和普通用户的授权边界。
- 登录成功、密码错误和账号不存在。
- 退出登录的幂等性。
- 知识库解析、切分、Embedding 和向量数据库写入。
- 向量化失败、重试和失败状态记录。
- 向量化任务幂等键和重复任务拦截。
- 重试耗尽后的最终失败处理。
- 重新处理时旧向量清理和新向量写入。
- Skill 创建、内容校验和多个知识库关联。
- 数字员工多个 Skill 关联。
- 数字员工发布前置校验。
- 发布版本号生成和配置快照。
- 历史发布版本不可变。
- 管理员回滚到历史版本并生成新的生效记录。
- 管理端预览对话：加载数字员工当前配置、按 Skills 展开 READY 知识库检索、组装上下文并一次性生成回答；检索/生成失败、无可用知识库与对话模型未配置的错误路径。

## 3. 集成测试

至少覆盖：

- Controller 到 Service 的接口调用。
- 数据库读写和 MyBatis-Plus 映射。
- Spring AI Embedding 和 `VectorStore` 的适配集成。
- 登录后访问受保护接口。
- 普通用户调用管理接口时被拒绝。
- 管理员可以查看和维护全局共享资源，不按创建者过滤。
- 知识库未完成向量化时不能被 Skill 关联。
- 所选 Skill 关联的知识库未完成时发布被拒绝。
- 相同文件版本不会创建重复向量化任务。
- 向量化重试不会产生重复向量片段。
- 回滚不会修改历史发布快照。

**管理端预览对话集成测试**：

- 已登录管理员可调用对话接口并收到一次性完整回复，用户问题与回复落库。
- 对话接口可延续已有会话或创建新会话；会话/消息历史接口可查询。
- 普通用户调用对话接口被拒绝（`RESOURCE_FORBIDDEN`）。
- 对话模型未配置时返回 `CHAT_PROVIDER_NOT_CONFIGURED`。

## 4. 测试数据原则

- 测试数据使用独立数据库或事务回滚。
- 不依赖真实第三方 API。
- HTTP 外部调用使用 Mock 的 `ApiClient`。
- 测试中不得写入真实密码或真实密钥。

## 5. 功能验收标准

### 用户

- 受控流程可以创建管理员账号。
- 普通用户注册不能自行设置管理员角色。
- 重复账号注册失败。
- 正确账号密码可以登录。
- 错误凭证不能登录。
- 退出后访问受保护接口失败。
- 普通用户不能访问管理接口。

### 知识库

- 可以上传合法文件。
- 非法文件类型或超限文件被拒绝。
- 管理员上传成功后可以在共享列表中查看。
- 上传后可以进入向量化处理状态。
- 向量化成功后状态变为 `READY`。
- 处理失败时可以看到失败状态。

### Skills

- 可以新增合法 Skill。
- 可以为 Skill 选择多个已完成的知识库。
- 缺少必填字段时创建失败。
- 管理员创建成功后可以在共享列表中查看 Skill。

### 数字员工

- 管理员可以创建数字员工。
- 可以关联多个 Skills。
- 创建数字员工时不需要直接选择知识库。
- 管理员可以查看共享数字员工及发布状态。
- 缺少必要配置时不能发布。
- 关联资源未准备完成时不能发布。
- 配置完整时可以成功发布并生成版本。

### 管理端预览对话

- 管理员可在预览页发起实时对话并收到一次性完整回复。
- 对话基于数字员工当前配置与关联 READY 知识库检索生成。
- 对话为空消息可被拦截；发送期间防重复提交。
- 对话可按会话延续，历史会话与消息可查询。
- 普通用户不可调用对话接口。

## 6. 发布前检查

```text
□ 编译通过
□ 单元测试通过
□ 集成测试通过
□ 覆盖率达到 80%
□ 无 System.out
□ 无 printStackTrace
□ HTTP 调用均使用 ApiClient
□ 单文件不超过 300 行
□ 单方法不超过 50 行
□ 新增 Java 文件包含文件头说明
```
