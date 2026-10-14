# 开发规范

## 1. 技术要求

- JDK 17
- MySQL 8.0
- Maven 3.9.3
- MyBatis-Plus 3.5.x
- Spring Boot 4.0.x

## 2. 分层规范

必须遵循：

```text
domain → config → mapper → service → controller
```

- Controller 不直接调用 Mapper。
- Service 负责业务规则和事务边界。
- Mapper 只负责数据访问。
- Domain 不依赖 Controller。
- 配置和基础设施能力集中放在 Config 或对应基础设施模块。

## 3. 日志规范

- 禁止使用 `System.out`。
- 禁止使用 `printStackTrace`。
- 统一使用 SLF4J。
- 使用结构化参数记录日志，例如：

```java
log.info("digital employee published, employeeId={}, version={}", employeeId, version);
```

- 不记录密码、访问令牌、API Key、完整提示词中的敏感数据。
- 异常日志包含上下文信息和异常对象。

## 4. 文件头规范

每个新增或修改的 Java 源文件头部必须包含文件说明。建议格式：

```java
/**
 * 文件说明：数字员工发布服务。
 *
 * @author
 * @since 2026-10-06
 */
```

文件说明应简洁准确，日期使用实际创建或首次修改日期。团队如果已有统一版权头，以团队模板为准。

## 5. 规模限制

- 单文件不超过 300 行。
- 单方法不超过 50 行。
- 复杂逻辑拆分为职责明确的私有方法或领域服务。
- 避免在 Controller 中堆积业务逻辑。

## 6. 依赖注入

- 使用构造器注入。
- 禁止在业务代码中直接实例化基础设施客户端。
- HTTP 调用统一使用 `ApiClient`。
- `ApiClient` 应由配置层创建并通过构造器注入到使用方。

## 7. 数据库规范

- 表名、字段名遵循项目统一命名风格。
- 所有业务表包含创建时间和更新时间。
- 知识库、Skill 和数字员工是全局共享资源，不添加个人 `owner_id` 过滤。
- 管理类接口必须校验管理员角色；资源关联校验资源存在和业务状态。
- 创建人、更新人和发布人从认证上下文获取，只用于审计，不能信任请求体中的操作者字段。
- 关键状态使用枚举或明确的常量，不使用散落的魔法字符串。

## 8. Git 提交建议

提交信息应说明变更目的，例如：

```text
feat: add digital employee publish flow
fix: validate shared knowledge base state
test: cover skill service
```

## 9. 工程事实（Spring Boot 4 差异）

实现过程中的框架差异记录，避免后续踩坑：

- JSON 序列化使用 Jackson 3（包名 `tools.jackson`，非 `com.fasterxml.jackson`）。
- MockMvc 测试支持独立 starter：`spring-boot-starter-webmvc-test`；`@AutoConfigureMockMvc` 位于 `org.springframework.boot.webmvc.test.autoconfigure`。
- Flyway 自动配置独立 starter：`spring-boot-starter-flyway`；**MySQL 支持需额外引入 `flyway-mysql`**（starter 默认不含）。
- actuator 健康检查 API 位于 `org.springframework.boot.health.*`（`AbstractHealthIndicator`/`Health.Builder`）。
- MyBatis-Plus 3.5.9+ 分页插件依赖独立模块 `mybatis-plus-jsqlparser`。
- Spring AI 2.0 的 `RedisVectorStore` 依赖 **Jedis 8+**（`redis.clients.jedis.RedisClient`，Boot BOM 默认 7.0.0 缺少该类），需设置 `spring.data.redis.client-type=jedis` 与 `spring.ai.vectorstore.redis.initialize-schema=true`。
- 用户 `@Configuration` 先于自动配置注册 Bean：缺省实现（本地模型/内存向量库）需用 `@ConditionalOnProperty` 门控（`local` 时创建），否则会抢先注册导致厂商自动配置的 `@ConditionalOnMissingBean` 失效。
- 单文件不超过 300 行约束在复杂服务上通过拆分职责组件满足（如向量化状态持久化、发布快照构建独立成组件）。
