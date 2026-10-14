# 部署与运行

## 1. 运行环境

- JDK 17
- Maven 3.9.3
- MySQL 8.0
- Spring Boot 4.0.x
- MyBatis-Plus 3.5.x
- 已配置的向量数据库
- 已配置的向量模型服务

具体依赖版本以项目 `pom.xml` 为准。

## 2. 必要配置

部署前必须配置：

- MySQL 连接地址、账号和密码
- 身份认证密钥或凭证配置
- 原始文件存储位置
- 向量数据库连接信息
- 向量模型服务地址、模型名称和 API Key
- 文件大小和允许类型
- 向量化任务重试次数和超时时间
- 日志级别和日志输出位置

敏感配置不得写入仓库中的配置文件。

**实现环境变量（通过环境注入，不写入仓库）**：

| 环境变量 | 用途 | 默认值 |
|---|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | MySQL 连接 | `jdbc:mysql://localhost:3306/harness_admin...` / `root` / 空（示例密码 `123456`） |
| `SERVER_PORT` | 服务端口 | `8080` |
| `KNOWLEDGE_BASE_DIR` | 知识库原始文件目录 | `./data/knowledge-bases` |
| `LOG_LEVEL` | 日志级别 | `INFO` |
| `harness.bootstrap-admin.username/password` | 初始管理员受控初始化（启动幂等创建） | 空（不创建） |
| `SPRING_AI_EMBEDDING_PROVIDER` | Embedding 提供方：`local`（本地缺省）或 `openai`（OpenAI 兼容，如阿里云百炼 qwen3） | `local` |
| `OPENAI_API_KEY` / `OPENAI_BASE_URL` / `OPENAI_EMBEDDING_MODEL` | OpenAI 兼容服务凭据与模型（`SPRING_AI_EMBEDDING_PROVIDER=openai` 时必填） | 空 / `https://api.openai.com/v1` / `text-embedding-3-small` |
| `SPRING_AI_VECTORSTORE_TYPE` | 向量库类型：`redis`（生产，需 RediSearch 模块）或 `local`（内存，仅开发） | `local` |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 连接（向量库） | `localhost` / `6379` / 空 |
| `REDIS_CLIENT_TYPE` | Redis 客户端类型（Spring AI 2.0 RedisVectorStore 依赖 Jedis） | `jedis` |
| `REDIS_VECTOR_INITIALIZE_SCHEMA` | 首次写入自动创建向量索引（FT.CREATE） | `true` |
| `REDIS_VECTOR_INDEX` / `REDIS_VECTOR_PREFIX` | Redis 向量索引名与键前缀（索引维度取自 Embedding 模型） | `harness_kb_index` / `kb:` |
| `harness.auth.token-expire-hours` 等 | 凭证有效期、登录防爆破 | `24` / `5` / `15` |
| `harness.storage.*` | 允许扩展名、大小上限 | `txt,md,pdf,docx` / `20MB` |
| `harness.vectorization.*` | 重试次数、退避基准、切分长度、模型/向量库标识 | `2` / `2` / `800` / `local-hash` / `simple-vector-store` |

**Redis 向量索引必备字段**：数字员工预览对话按「知识库（kb_id）分段检索」，因此 RediSearch 索引必须注册 `kb_id`（NUMERIC）与 `file_version`（TEXT）两个元数据字段。新索引由 `SpringAiConfig#redisVectorStore` 在自动创建时即包含；若为存量索引，需执行一次 `FT.ALTER` 补注册（数据无需重新向量化）：

```sql
FT.ALTER harness_kb_index SCHEMA ADD $.kb_id AS kb_id NUMERIC
FT.ALTER harness_kb_index SCHEMA ADD $.file_version AS file_version TEXT
```

完成后可用 `FT.SEARCH harness_kb_index '@kb_id:[13 13]'` 校验指定知识库能否按过滤检索。

表结构由 Flyway 版本化迁移自动执行（`src/main/resources/db/migration/V1..V6`），启动前确认数据库可连接。

**管理员账号创建（直接入库方式）**：`user` 表 `password_hash` 必须为 BCrypt 哈希（例如密码 `Admin@12345` 对应 `$2a$10$mk4mpZRSt4D.AyCILm/1MuIVvx9w1g6SfuyQkDmFS76G5Sgs6ajo2`，每次生成哈希不同，均有效）：

```sql
INSERT INTO `user` (`username`, `password_hash`, `role`, `status`)
VALUES ('admin', '$2a$10$mk4mpZRSt4D.AyCILm/1MuIVvx9w1g6SfuyQkDmFS76G5Sgs6ajo2', 'ADMIN', 'ACTIVE');
```

完整的配置模板见后端工程 `.env.example`。

## 3. 启动顺序

1. 准备 MySQL 数据库。
2. 执行数据库结构和索引迁移。
3. 准备原始文件存储。
4. 准备向量数据库和向量集合。
5. 配置向量模型。
6. 初始化第一个管理员账号。
7. 启动后端服务。
8. 检查健康状态和数据库连接。

## 4. 数据库迁移

- 所有表结构变更必须通过版本化迁移脚本完成。
- 禁止直接在生产库手工修改表结构。
- 迁移脚本必须可审计，并在测试环境先执行。
- 迁移失败时不得启动使用不完整结构的服务。
- 发布前确认新增索引不会造成长时间锁表。

## 5. 部署检查

```text
□ JDK、Maven 和数据库版本符合要求
□ MySQL 连接正常
□ 向量数据库连接正常
□ 向量模型调用正常
□ 管理员账号已初始化
□ 密钥未写入仓库
□ 数据库迁移成功
□ 健康检查通过
□ 日志和告警已接入
□ 向量化失败任务可查询
```

## 6. 备份与恢复

- MySQL 定期备份，并验证备份可恢复。
- 原始知识库文件必须独立备份。
- 向量数据库需要配置快照或重建方案。
- 恢复顺序建议为：MySQL、原始文件、向量数据库。
- 如果向量数据库无法恢复，应根据原始文件重新执行向量化任务。
- 恢复后必须校验知识库状态、向量数量和已发布版本引用。

## 7. 回滚

应用回滚和数字员工版本回滚分开处理：

- 应用回滚：恢复到上一份可运行的应用版本。
- 数字员工回滚：将当前生效版本切换到指定的历史发布快照。
- 数据库迁移回滚必须有明确脚本或恢复方案。
- 不允许通过修改历史发布快照内容实现回滚。
