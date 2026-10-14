/*
 Navicat Premium Dump SQL

 Source Server         : mysql
 Source Server Type    : MySQL
 Source Server Version : 80046 (8.0.46)
 Source Host           : localhost:3306
 Source Schema         : harness_admin

 Target Server Type    : MySQL
 Target Server Version : 80046 (8.0.46)
 File Encoding         : 65001

 Date: 13/10/2026 20:43:46
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for digital_employee
-- ----------------------------
DROP TABLE IF EXISTS `digital_employee`;
CREATE TABLE `digital_employee`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '数字员工主键',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '描述',
  `system_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '系统提示词',
  `model_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模型配置（JSON）',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '状态：DRAFT/PUBLISHED/ARCHIVED',
  `published_version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '当前发布版本',
  `created_by` bigint NOT NULL COMMENT '创建管理员 ID，仅用于审计',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of digital_employee
-- ----------------------------
INSERT INTO `digital_employee` VALUES (1, '??????', '????', '????????', '{\"model\":\"qwen3.7-text-embedding\"}', 'PUBLISHED', '3', 1, '2026-10-06 20:01:08', '2026-10-06 20:01:08');
INSERT INTO `digital_employee` VALUES (2, '售后助手', '处理售后咨询', '你是售后服务助手，结合知识库回答售后问题。', '{\"model\":\"deepseek-chat\",\"temperature\":0.7,\"maxTokens\":2048}', 'PUBLISHED', '1', 1, '2026-10-07 20:15:09', '2026-10-07 20:15:09');
INSERT INTO `digital_employee` VALUES (3, '测试员工', '', '测试提示词内容', '{\"model\":\"deepseek-chat\",\"temperature\":0.7,\"maxTokens\":2048}', 'PUBLISHED', '2', 1, '2026-10-07 20:16:21', '2026-10-07 20:16:21');
INSERT INTO `digital_employee` VALUES (4, 'Trim测试', '', '你是 Trim 测试助手，用于验证名称空白字符裁剪。', '{\"model\":\"deepseek-chat\",\"temperature\":0.7,\"maxTokens\":2048}', 'DRAFT', NULL, 1, '2026-10-07 20:28:14', '2026-10-07 20:28:14');
INSERT INTO `digital_employee` VALUES (5, '售后助手✨-A/B_1', '', '你是特殊字符测试助手，验证名称含 emoji 与特殊字符的保存展示。', '{\"model\":\"deepseek-chat\",\"temperature\":0.7,\"maxTokens\":2048}', 'DRAFT', NULL, 1, '2026-10-07 20:29:16', '2026-10-07 20:29:16');
INSERT INTO `digital_employee` VALUES (6, '防重复草稿', '', '你是防重复提交测试助手。', '{\"model\":\"deepseek-chat\",\"temperature\":0.7,\"maxTokens\":2048}', 'DRAFT', NULL, 1, '2026-10-07 20:30:08', '2026-10-07 20:30:08');
INSERT INTO `digital_employee` VALUES (7, '防重复草稿', '', '你是防重复提交测试助手。', '{\"model\":\"deepseek-chat\",\"temperature\":0.7,\"maxTokens\":2048}', 'DRAFT', NULL, 1, '2026-10-07 20:30:09', '2026-10-07 20:30:09');
INSERT INTO `digital_employee` VALUES (8, '防重复草稿2', '', '你是防重复提交测试助手。', '{\"model\":\"deepseek-chat\",\"temperature\":0.7,\"maxTokens\":2048}', 'DRAFT', NULL, 1, '2026-10-07 20:32:08', '2026-10-07 20:32:08');
INSERT INTO `digital_employee` VALUES (9, '发布防重测', '', '你是发布防重复测试助手。', '{\"model\":\"deepseek-chat\",\"temperature\":0.7,\"maxTokens\":2048}', 'PUBLISHED', '1', 1, '2026-10-07 20:33:00', '2026-10-07 20:33:00');
INSERT INTO `digital_employee` VALUES (10, '后端校验失败测', '', '你是后端校验失败测试助手。', '{\"model\":\"deepseek-chat\",\"temperature\":0.7,\"maxTokens\":2048}', 'DRAFT', NULL, 1, '2026-10-07 20:34:16', '2026-10-07 20:34:16');
INSERT INTO `digital_employee` VALUES (11, '测试数字员工', '这是测试用途', '当用户询问任何问题时，都要调用测试skill', '{\"model\":\"deepseek-chat\",\"temperature\":0.7,\"maxTokens\":2048}', 'PUBLISHED', '2', 1, '2026-10-08 08:20:42', '2026-10-08 08:20:42');
INSERT INTO `digital_employee` VALUES (12, '测试数字员工2', '测试用途', '当用户想要调用“测试2”技能时，就调用“测试2” skill', '{\"model\":\"deepseek-chat\",\"temperature\":0.7,\"maxTokens\":2048}', 'PUBLISHED', '1', 1, '2026-10-08 14:37:15', '2026-10-08 14:37:15');
INSERT INTO `digital_employee` VALUES (13, '测试数字员工3', '测试用途', '当用户想要调用测试3 skill时，就调用“测试3”skills，然后输出“测试3”skills里面的内容', '{\"model\":\"deepseek-chat\",\"temperature\":0.7,\"maxTokens\":2048}', 'PUBLISHED', '1', 1, '2026-10-08 14:40:08', '2026-10-08 14:40:08');
INSERT INTO `digital_employee` VALUES (14, '测试数字员工4', '测试用途', '当用户想要调用“测试4”技能时，就调用“测试4”skills，然后按照“测试4”skills输出内容', '{\"model\":\"deepseek-chat\",\"temperature\":0.7,\"maxTokens\":2048}', 'PUBLISHED', '1', 1, '2026-10-08 14:43:27', '2026-10-08 14:43:27');
INSERT INTO `digital_employee` VALUES (15, '????????', '????????????', '???????????????????????????AI?????????????????:1.??????????,??????;2.??????,?????????????;3.???????????????', '{\"model\":\"deepseek-chat\",\"temperature\":0.3,\"maxTokens\":2048}', 'PUBLISHED', '1', 1, '2026-10-08 16:02:45', '2026-10-08 16:02:45');
INSERT INTO `digital_employee` VALUES (16, '笔吧评测室', '负责回答笔记本的价格', '你是「商店员工」，一个只负责{回答笔记本电脑价格}的助手。\n\n规则：\n1. 只能依据下面提供的知识库资料作答，不得凭空编造。\n2. 资料能覆盖就直接引用并给结论；资料不足时明确说\"手册未覆盖\"，不要猜。\n3. 回答简洁、可直接执行。\n\n约束：不回答与{笔记本价格}无关的问题。', '{\"model\":\"deepseek-chat\",\"temperature\":0.7,\"maxTokens\":2048}', 'PUBLISHED', '1', 1, '2026-10-08 20:19:27', '2026-10-08 20:19:27');

-- ----------------------------
-- Table structure for digital_employee_release
-- ----------------------------
DROP TABLE IF EXISTS `digital_employee_release`;
CREATE TABLE `digital_employee_release`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '发布记录主键',
  `digital_employee_id` bigint NOT NULL COMMENT '数字员工 ID',
  `version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '发布版本号',
  `config_snapshot` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置快照（JSON，不可变）',
  `released_by` bigint NOT NULL COMMENT '发布人 ID，仅用于审计',
  `released_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '发布状态：SUCCESS 或 FAILED',
  `failure_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '失败原因',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_release_employee`(`digital_employee_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of digital_employee_release
-- ----------------------------
INSERT INTO `digital_employee_release` VALUES (1, 1, '1', '{\"systemPrompt\":\"????????\",\"modelConfig\":{\"model\":\"qwen3.7-text-embedding\"},\"skills\":[{\"skillId\":1,\"skillVersion\":\"1.0\"}],\"knowledgeBases\":[{\"knowledgeBaseId\":7,\"embeddingModel\":\"local-hash\",\"chunkCount\":1}]}', 1, '2026-10-06 20:01:08', 'SUCCESS', NULL);
INSERT INTO `digital_employee_release` VALUES (2, 1, '2', '{\"systemPrompt\":\"????????\",\"modelConfig\":{\"model\":\"qwen3.7-text-embedding\"},\"skills\":[{\"skillId\":1,\"skillVersion\":\"1.0\"}],\"knowledgeBases\":[{\"knowledgeBaseId\":7,\"embeddingModel\":\"local-hash\",\"chunkCount\":1}]}', 1, '2026-10-06 20:01:08', 'SUCCESS', NULL);
INSERT INTO `digital_employee_release` VALUES (3, 1, '3', '{\"systemPrompt\":\"????????\",\"modelConfig\":{\"model\":\"qwen3.7-text-embedding\"},\"skills\":[{\"skillId\":1,\"skillVersion\":\"1.0\"}],\"knowledgeBases\":[{\"knowledgeBaseId\":7,\"embeddingModel\":\"local-hash\",\"chunkCount\":1}]}', 1, '2026-10-06 20:01:08', 'SUCCESS', NULL);
INSERT INTO `digital_employee_release` VALUES (4, 3, '1', '{\"systemPrompt\":\"测试提示词内容\",\"modelConfig\":{\"model\":\"deepseek-chat\",\"temperature\":0.7,\"maxTokens\":2048},\"skills\":[{\"skillId\":2,\"skillVersion\":\"1.0\"}],\"knowledgeBases\":[{\"knowledgeBaseId\":8,\"embeddingModel\":\"local-hash\",\"chunkCount\":1},{\"knowledgeBaseId\":9,\"embeddingModel\":\"local-hash\",\"chunkCount\":1}]}', 1, '2026-10-07 20:16:21', 'SUCCESS', NULL);
INSERT INTO `digital_employee_release` VALUES (5, 3, '2', '{\"systemPrompt\":\"测试提示词内容\",\"modelConfig\":{\"model\":\"deepseek-chat\",\"temperature\":0.7,\"maxTokens\":2048},\"skills\":[{\"skillId\":2,\"skillVersion\":\"1.0\"}],\"knowledgeBases\":[{\"knowledgeBaseId\":8,\"embeddingModel\":\"local-hash\",\"chunkCount\":1},{\"knowledgeBaseId\":9,\"embeddingModel\":\"local-hash\",\"chunkCount\":1}]}', 1, '2026-10-07 20:17:26', 'SUCCESS', NULL);
INSERT INTO `digital_employee_release` VALUES (6, 2, '1', '{\"systemPrompt\":\"你是售后服务助手，结合知识库回答售后问题。\",\"modelConfig\":{\"model\":\"deepseek-chat\",\"temperature\":0.7,\"maxTokens\":2048},\"skills\":[{\"skillId\":1,\"skillVersion\":\"1.0\"},{\"skillId\":2,\"skillVersion\":\"1.0\"}],\"knowledgeBases\":[{\"knowledgeBaseId\":7,\"embeddingModel\":\"local-hash\",\"chunkCount\":1},{\"knowledgeBaseId\":8,\"embeddingModel\":\"local-hash\",\"chunkCount\":1},{\"knowledgeBaseId\":9,\"embeddingModel\":\"local-hash\",\"chunkCount\":1}]}', 1, '2026-10-07 20:20:04', 'SUCCESS', NULL);
INSERT INTO `digital_employee_release` VALUES (7, 9, '1', '{\"systemPrompt\":\"你是发布防重复测试助手。\",\"modelConfig\":{\"model\":\"deepseek-chat\",\"temperature\":0.7,\"maxTokens\":2048},\"skills\":[{\"skillId\":2,\"skillVersion\":\"1.0\"}],\"knowledgeBases\":[{\"knowledgeBaseId\":8,\"embeddingModel\":\"local-hash\",\"chunkCount\":1},{\"knowledgeBaseId\":9,\"embeddingModel\":\"local-hash\",\"chunkCount\":1}]}', 1, '2026-10-07 20:33:01', 'SUCCESS', NULL);
INSERT INTO `digital_employee_release` VALUES (8, 10, '1', '{}', 1, '2026-10-07 20:34:16', 'FAILED', '关联知识库尚未准备完成: 9');
INSERT INTO `digital_employee_release` VALUES (9, 2, '2', '{}', 1, '2026-10-07 20:35:04', 'FAILED', '关联知识库尚未准备完成: 9');
INSERT INTO `digital_employee_release` VALUES (10, 2, '3', '{}', 1, '2026-10-07 20:35:32', 'FAILED', '关联知识库尚未准备完成: 9');
INSERT INTO `digital_employee_release` VALUES (11, 11, '1', '{\"systemPrompt\":\"当用户询问任何问题时，都要调用测试skill\",\"modelConfig\":{\"model\":\"deepseek-chat\",\"temperature\":0.7,\"maxTokens\":2048},\"skills\":[{\"skillId\":3,\"skillVersion\":\"1.0\"}],\"knowledgeBases\":[{\"knowledgeBaseId\":12,\"embeddingModel\":\"local-hash\",\"chunkCount\":1}]}', 1, '2026-10-08 08:20:42', 'SUCCESS', NULL);
INSERT INTO `digital_employee_release` VALUES (12, 11, '2', '{\"systemPrompt\":\"当用户询问任何问题时，都要调用测试skill\",\"modelConfig\":{\"model\":\"deepseek-chat\",\"temperature\":0.7,\"maxTokens\":2048},\"skills\":[{\"skillId\":3,\"skillVersion\":\"1.0\"}],\"knowledgeBases\":[{\"knowledgeBaseId\":12,\"embeddingModel\":\"local-hash\",\"chunkCount\":1}]}', 1, '2026-10-08 14:33:00', 'SUCCESS', NULL);
INSERT INTO `digital_employee_release` VALUES (13, 12, '1', '{\"systemPrompt\":\"当用户想要调用“测试2”技能时，就调用“测试2” skill\",\"modelConfig\":{\"model\":\"deepseek-chat\",\"temperature\":0.7,\"maxTokens\":2048},\"skills\":[{\"skillId\":4,\"skillVersion\":\"1.0\"}],\"knowledgeBases\":[{\"knowledgeBaseId\":12,\"embeddingModel\":\"local-hash\",\"chunkCount\":1}]}', 1, '2026-10-08 14:37:16', 'SUCCESS', NULL);
INSERT INTO `digital_employee_release` VALUES (14, 13, '1', '{\"systemPrompt\":\"当用户想要调用测试3 skill时，就调用“测试3”skills，然后输出“测试3”skills里面的内容\",\"modelConfig\":{\"model\":\"deepseek-chat\",\"temperature\":0.7,\"maxTokens\":2048},\"skills\":[{\"skillId\":5,\"skillVersion\":\"1.0\"}],\"knowledgeBases\":[{\"knowledgeBaseId\":12,\"embeddingModel\":\"local-hash\",\"chunkCount\":1}]}', 1, '2026-10-08 14:40:08', 'SUCCESS', NULL);
INSERT INTO `digital_employee_release` VALUES (15, 14, '1', '{\"systemPrompt\":\"当用户想要调用“测试4”技能时，就调用“测试4”skills，然后按照“测试4”skills输出内容\",\"modelConfig\":{\"model\":\"deepseek-chat\",\"temperature\":0.7,\"maxTokens\":2048},\"skills\":[{\"skillId\":6,\"skillVersion\":\"1.0\"}],\"knowledgeBases\":[{\"knowledgeBaseId\":12,\"embeddingModel\":\"local-hash\",\"chunkCount\":1}]}', 1, '2026-10-08 14:43:27', 'SUCCESS', NULL);
INSERT INTO `digital_employee_release` VALUES (16, 15, '1', '{\"systemPrompt\":\"???????????????????????????AI?????????????????:1.??????????,??????;2.??????,?????????????;3.???????????????\",\"modelConfig\":{\"model\":\"deepseek-chat\",\"temperature\":0.3,\"maxTokens\":2048},\"skills\":[{\"skillId\":7,\"skillVersion\":\"1.0\"}],\"knowledgeBases\":[{\"knowledgeBaseId\":13,\"embeddingModel\":\"local-hash\",\"chunkCount\":1}]}', 1, '2026-10-08 16:17:27', 'SUCCESS', NULL);
INSERT INTO `digital_employee_release` VALUES (17, 16, '1', '{\"systemPrompt\":\"你是「商店员工」，一个只负责{回答笔记本电脑价格}的助手。\\n\\n规则：\\n1. 只能依据下面提供的知识库资料作答，不得凭空编造。\\n2. 资料能覆盖就直接引用并给结论；资料不足时明确说\\\"手册未覆盖\\\"，不要猜。\\n3. 回答简洁、可直接执行。\\n\\n约束：不回答与{笔记本价格}无关的问题。\",\"modelConfig\":{\"model\":\"deepseek-chat\",\"temperature\":0.7,\"maxTokens\":2048},\"skills\":[{\"skillId\":8,\"skillVersion\":\"1.0\"}],\"knowledgeBases\":[{\"knowledgeBaseId\":14,\"embeddingModel\":\"local-hash\",\"chunkCount\":6}]}', 1, '2026-10-08 20:19:27', 'SUCCESS', NULL);

-- ----------------------------
-- Table structure for digital_employee_skill
-- ----------------------------
DROP TABLE IF EXISTS `digital_employee_skill`;
CREATE TABLE `digital_employee_skill`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联主键',
  `digital_employee_id` bigint NOT NULL COMMENT '数字员工 ID',
  `skill_id` bigint NOT NULL COMMENT 'Skill ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_employee_skill`(`digital_employee_id` ASC, `skill_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of digital_employee_skill
-- ----------------------------
INSERT INTO `digital_employee_skill` VALUES (1, 1, 1);
INSERT INTO `digital_employee_skill` VALUES (3, 2, 1);
INSERT INTO `digital_employee_skill` VALUES (2, 2, 2);
INSERT INTO `digital_employee_skill` VALUES (4, 3, 2);
INSERT INTO `digital_employee_skill` VALUES (5, 4, 2);
INSERT INTO `digital_employee_skill` VALUES (6, 5, 2);
INSERT INTO `digital_employee_skill` VALUES (7, 6, 2);
INSERT INTO `digital_employee_skill` VALUES (8, 7, 2);
INSERT INTO `digital_employee_skill` VALUES (9, 8, 2);
INSERT INTO `digital_employee_skill` VALUES (10, 9, 2);
INSERT INTO `digital_employee_skill` VALUES (11, 10, 2);
INSERT INTO `digital_employee_skill` VALUES (12, 11, 3);
INSERT INTO `digital_employee_skill` VALUES (13, 12, 4);
INSERT INTO `digital_employee_skill` VALUES (14, 13, 5);
INSERT INTO `digital_employee_skill` VALUES (15, 14, 6);
INSERT INTO `digital_employee_skill` VALUES (16, 15, 7);
INSERT INTO `digital_employee_skill` VALUES (17, 16, 8);

-- ----------------------------
-- Table structure for flyway_schema_history
-- ----------------------------
DROP TABLE IF EXISTS `flyway_schema_history`;
CREATE TABLE `flyway_schema_history`  (
  `installed_rank` int NOT NULL,
  `version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `script` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `checksum` int NULL DEFAULT NULL,
  `installed_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`) USING BTREE,
  INDEX `flyway_schema_history_s_idx`(`success` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of flyway_schema_history
-- ----------------------------
INSERT INTO `flyway_schema_history` VALUES (1, '1', 'init user and auth token', 'SQL', 'V1__init_user_and_auth_token.sql', 1537090088, 'root', '2026-10-06 19:20:55', 79, 1);
INSERT INTO `flyway_schema_history` VALUES (2, '2', 'init knowledge base', 'SQL', 'V2__init_knowledge_base.sql', 141843069, 'root', '2026-10-06 19:20:55', 30, 1);
INSERT INTO `flyway_schema_history` VALUES (3, '3', 'init knowledge base task', 'SQL', 'V3__init_knowledge_base_task.sql', 1878739116, 'root', '2026-10-06 19:20:55', 57, 1);
INSERT INTO `flyway_schema_history` VALUES (4, '4', 'init skill and association', 'SQL', 'V4__init_skill_and_association.sql', -1486444979, 'root', '2026-10-06 19:20:55', 74, 1);
INSERT INTO `flyway_schema_history` VALUES (5, '5', 'init digital employee and association', 'SQL', 'V5__init_digital_employee_and_association.sql', -345279937, 'root', '2026-10-06 19:20:55', 62, 1);
INSERT INTO `flyway_schema_history` VALUES (6, '6', 'init digital employee release', 'SQL', 'V6__init_digital_employee_release.sql', -99620487, 'root', '2026-10-06 19:20:55', 32, 1);
INSERT INTO `flyway_schema_history` VALUES (7, '7', 'init preview chat', 'SQL', 'V7__init_preview_chat.sql', 1716441349, 'root', '2026-10-08 10:36:11', 262, 1);

-- ----------------------------
-- Table structure for knowledge_base
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_base`;
CREATE TABLE `knowledge_base`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '知识库主键',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '知识库名称',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '处理状态：UPLOADING/PROCESSING/READY/FAILED',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '原始文件名（规范化后）',
  `storage_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件存储标识（随机生成）',
  `chunk_count` int NOT NULL DEFAULT 0 COMMENT '文档片段数量',
  `vector_store` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '向量数据库标识',
  `embedding_model` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '向量模型标识',
  `failure_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '失败原因',
  `created_by` bigint NOT NULL COMMENT '创建管理员 ID，仅用于审计',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of knowledge_base
-- ----------------------------
INSERT INTO `knowledge_base` VALUES (1, '链路验证知识库', 'READY', 'harness_test_doc.txt', '0513b05017b64de688827d7587ae9f90.txt', 1, 'simple-vector-store', 'local-hash', NULL, 1, '2026-10-06 19:23:27', '2026-10-06 19:23:27');
INSERT INTO `knowledge_base` VALUES (2, '链路验证知识库2', 'READY', 'harness_test_doc.txt', 'a7a9f1a323044f2c93d2583feb8e136e.txt', 1, 'simple-vector-store', 'local-hash', NULL, 1, '2026-10-06 19:27:49', '2026-10-06 19:27:49');
INSERT INTO `knowledge_base` VALUES (3, 'Redis链路验证', 'READY', 'harness_test_doc.txt', 'a455dc40d6d84970999690ef7688c5a1.txt', 1, 'simple-vector-store', 'local-hash', NULL, 1, '2026-10-06 19:31:04', '2026-10-06 19:31:04');
INSERT INTO `knowledge_base` VALUES (4, 'Redis最终验证', 'READY', 'harness_test_doc.txt', '22fd322d77b94dda8a47554d7dd49859.txt', 1, 'simple-vector-store', 'local-hash', NULL, 1, '2026-10-06 19:35:43', '2026-10-06 19:35:43');
INSERT INTO `knowledge_base` VALUES (5, '全链路验证', 'READY', 'harness_test_doc.txt', 'f3bc9ead4e6a44ea9375c53995760306.txt', 1, 'simple-vector-store', 'local-hash', NULL, 1, '2026-10-06 19:41:37', '2026-10-06 19:41:37');
INSERT INTO `knowledge_base` VALUES (6, '最终全链路', 'READY', 'harness_test_doc.txt', 'ec5ee2ecece4415783141cb4927459ce.txt', 1, 'simple-vector-store', 'local-hash', NULL, 1, '2026-10-06 19:48:25', '2026-10-06 19:48:25');
INSERT INTO `knowledge_base` VALUES (7, '最终确认', 'READY', 'harness_test_doc.txt', 'c4cf150bcaaa44ab8b36d40e3c8c491f.txt', 1, 'simple-vector-store', 'local-hash', NULL, 1, '2026-10-06 19:52:09', '2026-10-06 19:52:09');
INSERT INTO `knowledge_base` VALUES (8, '客服知识库', 'READY', '客服知识库.txt', '6f6318798e134f4689ee3939badcf4c4.txt', 1, 'simple-vector-store', 'local-hash', NULL, 1, '2026-10-07 19:38:24', '2026-10-07 19:38:24');
INSERT INTO `knowledge_base` VALUES (9, '产品手册', 'READY', '产品手册.md', 'fe8d58ee7d1f40f9be3dda7f8d8a272c.md', 1, 'simple-vector-store', 'local-hash', NULL, 1, '2026-10-07 19:39:27', '2026-10-07 19:39:27');
INSERT INTO `knowledge_base` VALUES (10, '说明文档', 'FAILED', '说明文档.pdf', '8b3bb781e7164393aff20303e3310ff3.pdf', 0, NULL, NULL, '该文件类型暂不支持解析: pdf', 1, '2026-10-07 19:40:39', '2026-10-07 19:40:39');
INSERT INTO `knowledge_base` VALUES (11, '文档', 'FAILED', '文档.docx', 'f2b40ee2b24f4048b9a67a37820434e6.docx', 0, NULL, NULL, '该文件类型暂不支持解析: docx', 1, '2026-10-07 19:42:32', '2026-10-07 19:42:32');
INSERT INTO `knowledge_base` VALUES (12, '手动测试文档', 'READY', '手动测试文档.txt', '5aecb465d47c45ae85012d26df1d204e.txt', 1, 'simple-vector-store', 'local-hash', NULL, 1, '2026-10-08 08:18:36', '2026-10-08 08:18:36');
INSERT INTO `knowledge_base` VALUES (13, '平台员工手册-验证库', 'READY', 'harness_kb_验证.txt', '50e8e5dbebe7426db14d6a6f1f266926.txt', 1, 'simple-vector-store', 'local-hash', NULL, 1, '2026-10-08 16:02:01', '2026-10-08 16:02:01');
INSERT INTO `knowledge_base` VALUES (14, '笔记本电脑价格金砖版', 'READY', '笔记本电脑价格金砖版.txt', 'dd1f8cf1e2d2459a80cf708e185609ea.txt', 6, 'simple-vector-store', 'local-hash', NULL, 1, '2026-10-08 20:13:58', '2026-10-08 20:13:58');

-- ----------------------------
-- Table structure for knowledge_base_task
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_base_task`;
CREATE TABLE `knowledge_base_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务主键',
  `knowledge_base_id` bigint NOT NULL COMMENT '知识库 ID',
  `file_version` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '原始文件版本标识（存储键）',
  `idempotency_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '幂等键，唯一',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务状态：PENDING/RUNNING/SUCCESS/FAILED',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '已重试次数',
  `failure_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最后失败原因',
  `started_at` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `finished_at` datetime NULL DEFAULT NULL COMMENT '完成时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_task_idempotency_key`(`idempotency_key` ASC) USING BTREE,
  INDEX `idx_task_kb_id`(`knowledge_base_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of knowledge_base_task
-- ----------------------------
INSERT INTO `knowledge_base_task` VALUES (1, 1, '0513b05017b64de688827d7587ae9f90.txt', 'a1f10e7e7963469f273153315ba354133e3dbf676620fe94cb24cf099050ef04', 'SUCCESS', 0, NULL, '2026-10-06 19:23:28', '2026-10-06 19:23:30', '2026-10-06 19:23:27');
INSERT INTO `knowledge_base_task` VALUES (2, 2, 'a7a9f1a323044f2c93d2583feb8e136e.txt', '8f98985f5c6d8d3f3db13732b8b0e0aaa88ae4ee4ea7f5a5e2d792d5166f4735', 'SUCCESS', 0, NULL, '2026-10-06 19:27:49', '2026-10-06 19:27:50', '2026-10-06 19:27:49');
INSERT INTO `knowledge_base_task` VALUES (3, 3, 'a455dc40d6d84970999690ef7688c5a1.txt', '366a79c07372352948751ec81005f078f960aa19b8c17df1751182cf0d8231c6', 'SUCCESS', 0, NULL, '2026-10-06 19:31:05', '2026-10-06 19:31:06', '2026-10-06 19:31:04');
INSERT INTO `knowledge_base_task` VALUES (4, 4, '22fd322d77b94dda8a47554d7dd49859.txt', 'd2b5d6c7bdf834a333fb28a52dc333dc1e5d1d2a5e8c9a867ab79403140622c5', 'SUCCESS', 1, 'Failed to delete documents by filter', '2026-10-06 19:35:46', '2026-10-06 19:35:47', '2026-10-06 19:35:43');
INSERT INTO `knowledge_base_task` VALUES (5, 5, 'f3bc9ead4e6a44ea9375c53995760306.txt', 'f39d2ba4279353df080dcab5995bec035456a767dbc201a09f030f0771767176', 'SUCCESS', 1, 'Failed to delete documents by filter', '2026-10-06 19:41:40', '2026-10-06 19:41:40', '2026-10-06 19:41:37');
INSERT INTO `knowledge_base_task` VALUES (6, 6, 'ec5ee2ecece4415783141cb4927459ce.txt', 'cb2c87d84cc5a45e734f35641e3bdbab08f1a627af0888608b91e6c27fc7e359', 'SUCCESS', 1, 'Failed to delete documents by filter', '2026-10-06 19:48:30', '2026-10-06 19:48:30', '2026-10-06 19:48:25');
INSERT INTO `knowledge_base_task` VALUES (7, 6, 'ec5ee2ecece4415783141cb4927459ce.txt', '5c43eb605ed3e4f96e675fdb8043fdf1dca5cbf582147a6bfff8cbc2ca69fac4', 'SUCCESS', 1, 'Failed to delete documents by filter', '2026-10-06 19:48:32', '2026-10-06 19:48:33', '2026-10-06 19:48:30');
INSERT INTO `knowledge_base_task` VALUES (8, 7, 'c4cf150bcaaa44ab8b36d40e3c8c491f.txt', 'fb3abc2f862111c748e7690e7d1b5ce67f61c6d9fe3ff0aac05933f5b979eb5d', 'SUCCESS', 0, NULL, '2026-10-06 19:52:10', '2026-10-06 19:52:14', '2026-10-06 19:52:09');
INSERT INTO `knowledge_base_task` VALUES (9, 8, '6f6318798e134f4689ee3939badcf4c4.txt', '3464bd60bed5a797dc242522a31055772232985899307d6a07ec9f13ab63da53', 'SUCCESS', 0, NULL, '2026-10-07 19:38:25', '2026-10-07 19:38:25', '2026-10-07 19:38:24');
INSERT INTO `knowledge_base_task` VALUES (10, 9, 'fe8d58ee7d1f40f9be3dda7f8d8a272c.md', 'd500e29bf4e023536b4aa27b47f6d1d66339224302c26c7c8c6fa4c3caaa5110', 'SUCCESS', 0, NULL, '2026-10-07 19:39:28', '2026-10-07 19:39:28', '2026-10-07 19:39:27');
INSERT INTO `knowledge_base_task` VALUES (11, 10, '8b3bb781e7164393aff20303e3310ff3.pdf', '7a064916279de737293e41c3a83d004570965db83df9cd36e5553b50e717e8ad', 'FAILED', 2, '该文件类型暂不支持解析: pdf', '2026-10-07 19:40:46', '2026-10-07 19:40:46', '2026-10-07 19:40:39');
INSERT INTO `knowledge_base_task` VALUES (12, 11, 'f2b40ee2b24f4048b9a67a37820434e6.docx', '16820cd6b803b5186f2f9b786f07c735694a96cfdb4a1072c45b6b41bdb86280', 'FAILED', 2, '该文件类型暂不支持解析: docx', '2026-10-07 19:42:38', '2026-10-07 19:42:38', '2026-10-07 19:42:32');
INSERT INTO `knowledge_base_task` VALUES (13, 8, '6f6318798e134f4689ee3939badcf4c4.txt', 'beada89fefde04aa2105ef6de5bbb51dc9056d3797423250402b089d539d3e84', 'SUCCESS', 0, NULL, '2026-10-07 19:45:16', '2026-10-07 19:45:18', '2026-10-07 19:45:16');
INSERT INTO `knowledge_base_task` VALUES (14, 10, '8b3bb781e7164393aff20303e3310ff3.pdf', '1c4fe5b59845c4b55df9c4602c6965856da8fe904ef5b2ac7ec9522b5c5cb8eb', 'FAILED', 2, '该文件类型暂不支持解析: pdf', '2026-10-07 19:46:00', '2026-10-07 19:46:00', '2026-10-07 19:45:54');
INSERT INTO `knowledge_base_task` VALUES (15, 12, '5aecb465d47c45ae85012d26df1d204e.txt', '15e1130030cf3974d3e7e48318e629b3dd6ff4f52d0afc5797537e6acf54fc08', 'SUCCESS', 0, NULL, '2026-10-08 08:18:36', '2026-10-08 08:18:38', '2026-10-08 08:18:36');
INSERT INTO `knowledge_base_task` VALUES (16, 13, '50e8e5dbebe7426db14d6a6f1f266926.txt', '49df5c8e767cc47c15f790b615f0dd5ed8a13a947ccece3a13607f3456c5142b', 'SUCCESS', 0, NULL, '2026-10-08 16:02:01', '2026-10-08 16:02:03', '2026-10-08 16:02:01');
INSERT INTO `knowledge_base_task` VALUES (17, 14, 'dd1f8cf1e2d2459a80cf708e185609ea.txt', 'a424b2d96d229659087a7b3950f8714ebea9cd9772074c4b268568a4834d1007', 'SUCCESS', 0, NULL, '2026-10-08 20:13:59', '2026-10-08 20:14:00', '2026-10-08 20:13:58');
INSERT INTO `knowledge_base_task` VALUES (18, 14, 'dd1f8cf1e2d2459a80cf708e185609ea.txt', '2e3fc85c117768614bc9784f4903c13198ce35aecb63ccd00b9e78ab48d96b30', 'SUCCESS', 0, NULL, '2026-10-13 10:46:31', '2026-10-13 10:46:34', '2026-10-13 10:46:31');
INSERT INTO `knowledge_base_task` VALUES (19, 14, 'dd1f8cf1e2d2459a80cf708e185609ea.txt', '0d4d17f38e6e63ec8f0106706b1f44d27c70c9cb32a61a5a2a1854c2e12feb6f', 'SUCCESS', 0, NULL, '2026-10-13 19:25:30', '2026-10-13 19:25:32', '2026-10-13 19:25:30');

-- ----------------------------
-- Table structure for preview_conversation
-- ----------------------------
DROP TABLE IF EXISTS `preview_conversation`;
CREATE TABLE `preview_conversation`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话主键',
  `digital_employee_id` bigint NOT NULL COMMENT '数字员工 ID（对话来源）',
  `title` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '会话标题（取首条问题摘要）',
  `created_by` bigint NOT NULL COMMENT '发起会话的管理员 ID，仅用于审计',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近消息时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_conversation_employee`(`digital_employee_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 38 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of preview_conversation
-- ----------------------------
INSERT INTO `preview_conversation` VALUES (2, 3, '请用一句话说明你可以提供什么帮助', 1, '2026-10-08 14:25:52', '2026-10-08 14:26:20');
INSERT INTO `preview_conversation` VALUES (3, 11, '你好', 1, '2026-10-08 14:31:39', '2026-10-08 14:34:14');
INSERT INTO `preview_conversation` VALUES (4, 12, '你好', 1, '2026-10-08 14:37:22', '2026-10-08 14:37:33');
INSERT INTO `preview_conversation` VALUES (5, 13, '你好', 1, '2026-10-08 14:40:12', '2026-10-08 14:40:23');
INSERT INTO `preview_conversation` VALUES (6, 14, '你好', 1, '2026-10-08 14:43:32', '2026-10-08 14:44:04');
INSERT INTO `preview_conversation` VALUES (7, 5, '你好', 1, '2026-10-08 14:44:53', '2026-10-08 14:44:53');
INSERT INTO `preview_conversation` VALUES (8, 14, '知识库“手动测试文档”中的内容是什么？', 1, '2026-10-08 14:46:23', '2026-10-08 14:47:10');
INSERT INTO `preview_conversation` VALUES (9, 15, '????????:???????????', 1, '2026-10-08 16:03:03', '2026-10-08 16:03:03');
INSERT INTO `preview_conversation` VALUES (10, 15, '????????:???????????', 1, '2026-10-08 16:03:55', '2026-10-08 16:03:55');
INSERT INTO `preview_conversation` VALUES (11, 15, '依据员工手册回答：公司给员工的午餐补贴标', 1, '2026-10-08 16:05:10', '2026-10-08 16:05:10');
INSERT INTO `preview_conversation` VALUES (12, 14, '这份知识库的主要内容是关于什么的？请结合', 1, '2026-10-08 16:17:45', '2026-10-08 16:17:45');
INSERT INTO `preview_conversation` VALUES (15, 15, '依据员工手册回答：公司给员工的午餐补贴标', 1, '2026-10-08 20:03:21', '2026-10-08 20:03:21');
INSERT INTO `preview_conversation` VALUES (16, 15, '住宿一晚花了720元，按员工手册能报销多', 1, '2026-10-08 20:03:41', '2026-10-08 20:03:41');
INSERT INTO `preview_conversation` VALUES (17, 15, '公司股票期权该怎么申请？', 1, '2026-10-08 20:03:45', '2026-10-08 20:03:45');
INSERT INTO `preview_conversation` VALUES (18, 15, '午餐补贴', 1, '2026-10-08 20:04:49', '2026-10-08 20:05:00');
INSERT INTO `preview_conversation` VALUES (19, 16, '你好', 1, '2026-10-08 20:19:31', '2026-10-08 20:19:51');
INSERT INTO `preview_conversation` VALUES (20, 16, '????????', 1, '2026-10-12 10:02:34', '2026-10-12 10:02:34');
INSERT INTO `preview_conversation` VALUES (21, 16, '介绍一下你的能力', 1, '2026-10-12 10:30:56', '2026-10-12 10:30:56');
INSERT INTO `preview_conversation` VALUES (22, 16, '今天有什么进展可以汇报', 1, '2026-10-12 10:32:14', '2026-10-12 10:34:35');
INSERT INTO `preview_conversation` VALUES (23, 16, 'y9000p的价格是多少', 1, '2026-10-13 10:22:05', '2026-10-13 10:45:43');
INSERT INTO `preview_conversation` VALUES (24, 16, '??', 6, '2026-10-13 10:25:57', '2026-10-13 10:25:57');
INSERT INTO `preview_conversation` VALUES (25, 16, '你好，介绍一下你擅长的领域', 7, '2026-10-13 10:39:28', '2026-10-13 10:39:28');
INSERT INTO `preview_conversation` VALUES (26, 16, '你好', 1, '2026-10-13 10:46:02', '2026-10-13 10:46:11');
INSERT INTO `preview_conversation` VALUES (27, 16, '你好', 1, '2026-10-13 10:46:45', '2026-10-13 10:48:03');
INSERT INTO `preview_conversation` VALUES (28, 16, '你好', 5, '2026-10-13 19:20:51', '2026-10-13 19:23:20');
INSERT INTO `preview_conversation` VALUES (29, 16, '你好', 1, '2026-10-13 19:24:55', '2026-10-13 19:25:23');
INSERT INTO `preview_conversation` VALUES (30, 16, '你好', 1, '2026-10-13 19:25:39', '2026-10-13 19:25:52');
INSERT INTO `preview_conversation` VALUES (31, 16, '你好', 1, '2026-10-13 19:32:57', '2026-10-13 19:33:12');
INSERT INTO `preview_conversation` VALUES (32, 16, '?? ????? Y9000P 2024', 1, '2026-10-13 19:46:41', '2026-10-13 19:46:41');
INSERT INTO `preview_conversation` VALUES (33, 16, '?? ????? Y9000P 2024', 1, '2026-10-13 19:46:57', '2026-10-13 19:46:57');
INSERT INTO `preview_conversation` VALUES (34, 16, '请问 联想拯救者 Y9000P 2024', 1, '2026-10-13 19:47:27', '2026-10-13 19:47:27');
INSERT INTO `preview_conversation` VALUES (35, 16, '笔记本 3060 显卡大概什么价位？', 1, '2026-10-13 19:47:30', '2026-10-13 19:47:30');
INSERT INTO `preview_conversation` VALUES (36, 16, '你好', 1, '2026-10-13 20:38:18', '2026-10-13 20:38:40');
INSERT INTO `preview_conversation` VALUES (37, 16, '你好', 5, '2026-10-13 20:41:23', '2026-10-13 20:41:54');

-- ----------------------------
-- Table structure for preview_message
-- ----------------------------
DROP TABLE IF EXISTS `preview_message`;
CREATE TABLE `preview_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息主键',
  `conversation_id` bigint NOT NULL COMMENT '所属会话 ID',
  `role` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息角色：USER 或 ASSISTANT',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_message_conversation`(`conversation_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 133 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of preview_message
-- ----------------------------
INSERT INTO `preview_message` VALUES (2, 2, 'USER', '请用一句话说明你可以提供什么帮助', '2026-10-08 14:25:52');
INSERT INTO `preview_message` VALUES (3, 2, 'ASSISTANT', '我可以提供问答、写作、翻译、代码与数据分析等帮助。', '2026-10-08 14:25:57');
INSERT INTO `preview_message` VALUES (4, 2, 'USER', '好的，请继续', '2026-10-08 14:26:20');
INSERT INTO `preview_message` VALUES (5, 2, 'ASSISTANT', '好的，已收到您的测试内容。  \n请问您希望我如何继续？  \n可以告诉我：  \n- 需要测试哪种场景（如问答、生成、逻辑推理等）？  \n- 是否有特定格式或要求？  \n- 需要我直接提供示例回答吗？  \n\n我会根据您的指示灵活配合。', '2026-10-08 14:26:26');
INSERT INTO `preview_message` VALUES (6, 3, 'USER', '你好', '2026-10-08 14:31:39');
INSERT INTO `preview_message` VALUES (7, 3, 'ASSISTANT', '你好！👋\n\n很抱歉，我目前可用的工具列表中并没有\"测试skill\"这个工具，因此无法进行调用。\n\n不过，你随时可以向我提问或聊天，我会尽力帮助你！有什么我可以为你做的吗？😊', '2026-10-08 14:31:44');
INSERT INTO `preview_message` VALUES (8, 3, 'USER', '仅调用“测试”即可', '2026-10-08 14:33:26');
INSERT INTO `preview_message` VALUES (9, 3, 'ASSISTANT', '当前对话中，我没有可用的\"测试\"技能（skill）或相关工具可以调用。我无法执行此操作。\n\n如果您需要我调用某个特定的技能或工具，请确认该技能已在当前环境中注册并可用。您可以提供更多上下文，例如：\n\n- 该\"测试\"技能属于哪个平台或插件系统？\n- 您是否在测试某种工作流或自动化流程？\n\n这样我可以更好地协助您。', '2026-10-08 14:33:30');
INSERT INTO `preview_message` VALUES (10, 3, 'USER', '你可以调用哪些技能', '2026-10-08 14:33:43');
INSERT INTO `preview_message` VALUES (11, 3, 'ASSISTANT', '已调用测试 skill ✅\n\n我可以调用的技能包括：\n\n1. **测试 skill**  \n   - 用于执行用户要求的测试调用、模拟工具调用、检查调用流程。\n\n2. **通用问答技能**  \n   - 回答知识性问题、概念解释、对比分析、总结归纳等。\n\n3. **写作与润色技能**  \n   - 撰写文章、邮件、报告、文案、标题、摘要、改写润色等。\n\n4. **代码相关技能**  \n   - 代码解释、调试、生成、优化、单元测试建议、API 使用示例等。\n\n5. **数据分析与推理技能**  \n   - 对文本、表格数据、逻辑问题进行推理、统计描述、趋势分析等。\n\n6. **学习辅助技能**  \n   - 制定学习计划、拆解知识点、生成练习题、讲解错题等。\n\n7. **多语言翻译与本地化技能**  \n   - 中英互译、多语种翻译、术语统一、语气本地化等。\n\n8. **对话与角色扮演技能**  \n   - 模拟面试官、客服、老师、产品经理、游戏 NPC 等对话场景。\n\n如果你需要，我也可以按具体场景调用这些技能来完成任务。', '2026-10-08 14:33:54');
INSERT INTO `preview_message` VALUES (12, 3, 'USER', '请调用**测试 skill**', '2026-10-08 14:34:14');
INSERT INTO `preview_message` VALUES (13, 3, 'ASSISTANT', '当前环境中没有可直接调用的名为 **测试 skill** 的工具或插件，因此无法完成“调用测试 skill”这一请求。\n\n如果你希望，我可以：\n1. 模拟一个“测试 skill”的执行流程；\n2. 根据你的测试内容帮你执行测试分析；\n3. 协助你把“测试 skill”接入为可调用的工具/API。', '2026-10-08 14:34:23');
INSERT INTO `preview_message` VALUES (14, 4, 'USER', '你好', '2026-10-08 14:37:22');
INSERT INTO `preview_message` VALUES (15, 4, 'ASSISTANT', '你好！👋 很高兴见到你。有什么我可以帮你的吗？', '2026-10-08 14:37:24');
INSERT INTO `preview_message` VALUES (16, 4, 'USER', '调用“测试2”', '2026-10-08 14:37:33');
INSERT INTO `preview_message` VALUES (17, 4, 'ASSISTANT', '已调用“测试2” skill。', '2026-10-08 14:37:49');
INSERT INTO `preview_message` VALUES (18, 5, 'USER', '你好', '2026-10-08 14:40:12');
INSERT INTO `preview_message` VALUES (19, 5, 'ASSISTANT', '你好！有什么我可以帮你的吗？', '2026-10-08 14:40:15');
INSERT INTO `preview_message` VALUES (20, 5, 'USER', '调用“测试3”', '2026-10-08 14:40:23');
INSERT INTO `preview_message` VALUES (21, 5, 'ASSISTANT', '测试3\n\n**功能说明**  \n本技能用于执行基础测试流程，验证系统调用、响应生成及内容输出功能。\n\n**执行步骤**  \n1. 识别用户请求中指定的技能名称（如“测试3”）。  \n2. 加载对应技能内容模板。  \n3. 按规范格式输出结果。  \n\n**示例响应**  \n当用户输入“调用‘测试3’”时，系统应返回本内容。  \n\n**注意事项**  \n- 技能名称需完全匹配引号内文本。  \n- 输出内容不包含额外解释或错误信息。', '2026-10-08 14:40:30');
INSERT INTO `preview_message` VALUES (22, 6, 'USER', '你好', '2026-10-08 14:43:32');
INSERT INTO `preview_message` VALUES (23, 6, 'ASSISTANT', '你好！有什么我可以帮你的吗？', '2026-10-08 14:43:35');
INSERT INTO `preview_message` VALUES (24, 6, 'USER', '调用“测试4”', '2026-10-08 14:43:40');
INSERT INTO `preview_message` VALUES (25, 6, 'ASSISTANT', '很抱歉，当前环境中没有找到名为“测试4”的技能。\n\n为了帮您正确调用，请您提供更多信息：\n\n1. **“测试4”技能具体属于哪个平台或系统？**（例如：钉钉、微信小程序、企业微信、某个特定的 AI 开发平台等）\n2. **“测试4”技能的预期功能或输出内容是什么？**\n3. **是否有该技能的调用示例或相关文档？**\n\n请补充这些信息，我会尽力协助您完成调用。', '2026-10-08 14:43:49');
INSERT INTO `preview_message` VALUES (26, 6, 'USER', '调用“测试4”技能', '2026-10-08 14:44:04');
INSERT INTO `preview_message` VALUES (27, 6, 'ASSISTANT', '已调用“测试4”技能。  \n输出内容：测试4', '2026-10-08 14:44:14');
INSERT INTO `preview_message` VALUES (28, 7, 'USER', '你好', '2026-10-08 14:44:53');
INSERT INTO `preview_message` VALUES (29, 7, 'ASSISTANT', '你好！我是特殊字符测试助手，可验证名称中含 emoji、特殊符号、空格、中文、数字等的保存与展示效果。\n\n请提供你要测试的名称或字段内容，例如：\n\n- `🎉Test_01`\n- `张😀三 #Special`\n- `name@#$%^&*()`\n- `emoji ❤️ 保存测试`\n\n我可以帮助检查其是否可正常保存、展示是否异常、是否有转义/截断/乱码等问题。', '2026-10-08 14:44:58');
INSERT INTO `preview_message` VALUES (30, 8, 'USER', '知识库“手动测试文档”中的内容是什么？', '2026-10-08 14:46:23');
INSERT INTO `preview_message` VALUES (31, 8, 'ASSISTANT', '我无法查询知识库中“手动测试文档”的具体内容，因为该文档信息未在当前环境中提供。建议您确认文档是否已正确上传，或提供更多上下文以便进一步协助。', '2026-10-08 14:46:28');
INSERT INTO `preview_message` VALUES (32, 8, 'USER', '帮我介绍一下岗位职责', '2026-10-08 14:47:10');
INSERT INTO `preview_message` VALUES (33, 8, 'ASSISTANT', '可以，下面按“岗位职责”通用模板为你介绍。如果你是想调用“测试4”skills，请补充“测试4”skills 的具体输出规则或内容，这样我可以完全按该技能格式输出。\n\n---\n\n## 岗位职责介绍模板\n\n### 一、岗位定位\n\n该岗位主要负责围绕本部门的业务目标，完成相关工作内容的计划、执行、协调、监督和优化，确保工作流程高效运转，并为团队或公司提供稳定支持。\n\n### 二、核心职责\n\n1. **日常业务执行**\n   - 负责岗位相关日常工作的推进与落实。\n   - 按照既定流程完成工作任务，保证工作质量和时效。\n\n2. **信息整理与沟通协作**\n   - 收集整理岗位相关信息、数据和资料。\n   - 与相关部门或人员保持沟通，协调资源，推动事项落地。\n\n3. **进度跟踪与反馈**\n   - 对负责事项进行跟踪，及时汇报进展。\n   - 发现问题、风险或异常时，主动反馈并提出解决方案。\n\n4. **文档与记录管理**\n   - 建立并维护岗位相关的文档、台账或工作记录。\n   - 确保资料完整、可追溯，便于后续查询和交接。\n\n5. **流程优化与支持**\n   - 根据实际工作情况，提出流程优化建议。\n   - 配合上级或团队完成专项任务、临时性工作及其他支持性事务。\n\n### 三、工作权限\n\n- 对岗位内职责事项进行推进和跟进。\n- 在授权范围内进行资料查询、信息整理、沟通协调和流程执行。\n- 重大事项需请示上级或按照公司制度执行。\n\n### 四、协作关系\n\n- **内部协作**：与本部门同事、上级主管及其他相关部门保持沟通。\n- **外部协作**：如涉及客户、供应商、合作伙伴等，按公司要求进行对接与协调。\n- **汇报对象**：通常直接向岗位上级或部门负责人汇报工作。\n\n### 五、考核重点\n\n岗位职责落实情况通常可通过以下维度进行考核：\n\n1. 工作任务完成率  \n2. 工作质量与准确性  \n3. 沟通协调能力  \n4. 问题发现与解决能力  \n5. 文档与流程规范执行情况  \n6. 团队协作与执行力  \n\n### 六、任职要求参考\n\n- 具备岗位所需的专业知识或业务基础。\n- 能够熟练使用相关办公工具或系统。\n- 具备良好的责任心、沟通能力和执行力。\n- 能够适应团队协作和一定程度的多任务并行工作。\n\n---\n\n如果你需要，我还可以继续帮你生成一份更正式的版本，例如：  \n- **通用岗位职责说明书**  \n- **具体岗位的职责描述**，如行政、HR、运营、产品、销售、财务、技术、客服等岗位  \n- **适合招聘网站展示的职责描述**  \n- **适合公司制度文件的岗位职责模板**', '2026-10-08 14:47:35');
INSERT INTO `preview_message` VALUES (34, 9, 'USER', '????????:??????????????', '2026-10-08 16:03:03');
INSERT INTO `preview_message` VALUES (35, 9, 'ASSISTANT', '抱歉，您发送的内容在传输/显示过程中可能出现了编码问题，所有文字都变成了“?”，因此我无法准确判断原问题是什么。\n\n如果您是在询问 **“AI 发展面临的问题/挑战”**，一般可以从以下几个方面回答：\n\n1. **技术与数据问题**  \n   例如：算力成本高、模型训练需要大量数据、数据质量不足、模型可解释性差、存在幻觉或错误输出、隐私保护难度大等。\n\n2. **伦理与治理问题**  \n   例如：算法偏见、歧视风险、责任归属不清、监管制度滞后、安全风险、深度伪造滥用、对个人信息保护的挑战等。\n\n3. **社会与经济问题**  \n   例如：对就业结构的影响、产业转型压力、数字鸿沟加剧、人才短缺、跨国竞争与合作问题等。\n\n如果您需要的是 **翻译、润色、问答或分析某段材料**，请重新发送原文，我会继续为您解答。', '2026-10-08 16:03:26');
INSERT INTO `preview_message` VALUES (36, 10, 'USER', '????????:?????????????????????', '2026-10-08 16:03:55');
INSERT INTO `preview_message` VALUES (37, 10, 'ASSISTANT', '非常抱歉，你发送的内容目前显示为乱码（大量“?”），我无法准确还原“用户问题”的具体意思，因此不能按原问题精确作答。\n\n你可以重新发一下，并建议：\n\n1. 用普通中文文本直接粘贴，不要使用特殊编码或二进制格式；\n2. 如果内容来自文件，请将文件编码改为 UTF-8 后再复制；\n3. 如果原问题包含“AI、三个要点、用户问题”等关键词，请尽量完整写出，例如：“请围绕 AI 给出三点说明：1.……；2.……；3.……”。\n\n如果你希望我先按这个乱码的大致方向给一个通用回答，我也可以先提供这样一个版本：\n\n1. 明确 AI 是什么：它可以是基于数据、算法和模型进行感知、推理、生成和决策的工具，不是完全等同于人类智能的自主意识体。  \n2. 说明 AI 的边界：AI 擅长处理模式识别、文本生成、图像理解、预测分析等任务，但在事实准确性、价值判断、责任承担等方面仍需人类审核与约束。  \n3. 给出使用建议：在使用 AI 时应明确需求、保留判断、核验结果，并重视隐私、版权、安全和伦理风险。\n\n请把你真正想问的那句话重新发一遍，我会按你的问题直接、准确地回答。', '2026-10-08 16:04:28');
INSERT INTO `preview_message` VALUES (38, 11, 'USER', '依据员工手册回答：公司给员工的午餐补贴标准是每人每天多少钱？', '2026-10-08 16:05:10');
INSERT INTO `preview_message` VALUES (39, 11, 'ASSISTANT', '根据现有信息，我无法确定公司给员工的午餐补贴标准是每人每天多少钱，因为目前未提供《员工手册》中关于午餐补贴的具体条款或数据。\n\n请提供员工手册中“福利补贴 / 餐饮补贴 / 午餐补贴”相关内容，或上传手册文件/对应章节文本，我可依据其原文回答具体金额及适用规则。', '2026-10-08 16:05:21');
INSERT INTO `preview_message` VALUES (40, 12, 'USER', '这份知识库的主要内容是关于什么的？请结合知识库内容概括。', '2026-10-08 16:17:45');
INSERT INTO `preview_message` VALUES (41, 12, 'ASSISTANT', '我目前无法调用名为“测试4”的特定技能或功能，因为这超出了我当前可提供的服务范围。如果您有关于知识库内容概括的需求，请提供具体的资料文本、主题范围或关键信息点，我将基于以下内容特征为您生成清晰、准确且结构化的摘要：\n\n1. **内容范围**：知识库涉及的学科领域、行业类型或专题方向（如科技、医学、教育等）。\n2. **核心主题**：主要讨论的问题、解决方案、案例分析或数据结论。\n3. **结构特点**：是否包含理论框架、实践步骤、常见问题解答或参考文献。\n4. **时效性与权威性**：信息来源的发布时间、数据可靠性及适用场景。\n5. **用户目标**：您希望概括侧重总结、提炼要点、对比分析还是简化说明。\n\n例如，若您提供一份关于“人工智能技术发展”的文档，我可概括为：“该知识库系统梳理了2010年以来AI在机器学习、自然语言处理领域的突破，重点分析技术应用场景（如医疗影像诊断、智能推荐系统），并探讨伦理与数据隐私挑战。”\n\n请补充具体内容或上传相关文本，我将据此生成符合要求的概括结果。', '2026-10-08 16:17:57');
INSERT INTO `preview_message` VALUES (45, 15, 'USER', '依据员工手册回答：公司给员工的午餐补贴标准是每人每天多少钱？', '2026-10-08 20:03:21');
INSERT INTO `preview_message` VALUES (46, 15, 'ASSISTANT', '依据员工手册，公司给员工的午餐补贴标准是：**每人每天 30 元**。', '2026-10-08 20:03:26');
INSERT INTO `preview_message` VALUES (47, 16, 'USER', '住宿一晚花了720元，按员工手册能报销多少？', '2026-10-08 20:03:41');
INSERT INTO `preview_message` VALUES (48, 16, 'ASSISTANT', '按员工手册，住宿费用每晚上限为 **600 元**，超出部分自理。\n\n因此，住宿一晚花了 **720 元**，可报销金额为：\n\n**600 元**\n\n前提是需在费用发生之日起 7 天内提交报销申请，并附合规发票、发票抬头为公司全称。', '2026-10-08 20:03:45');
INSERT INTO `preview_message` VALUES (49, 17, 'USER', '公司股票期权该怎么申请？', '2026-10-08 20:03:45');
INSERT INTO `preview_message` VALUES (50, 17, 'ASSISTANT', '根据现有《深度工业AI平台员工手册（内部版）》资料，未包含“公司股票期权申请”的制度、申请条件、审批流程或办理入口，因此无法依据该手册给出具体操作办法。\n\n建议按以下途径确认：\n\n1. 联系人力资源部或员工关系/薪酬福利团队，询问公司是否有股权激励/股票期权制度文件及申请流程。  \n2. 如公司设有法务、财务或股权管理部门，可向相关部门索取期权授予对象、申请条件、协议模板、行权规则等材料。  \n3. 准备个人基础信息，例如工号、岗位、入职时间、绩效记录、所属部门等，便于判断是否具备申请或授予资格。  \n4. 若手册以外的政策存在，请以公司正式发布制度、邮件通知或期权授予协议为准。\n\n目前检索到的制度仅涉及假期、报销、福利、办公规范和信息安全，未涉及股票期权，因此不能据此确认你可以申请或如何申请。', '2026-10-08 20:03:54');
INSERT INTO `preview_message` VALUES (51, 18, 'USER', '午餐补贴', '2026-10-08 20:04:49');
INSERT INTO `preview_message` VALUES (52, 18, 'ASSISTANT', '公司为员工提供午餐补贴，标准为 **每人每天 30 元**。', '2026-10-08 20:04:52');
INSERT INTO `preview_message` VALUES (53, 18, 'USER', '帮我介绍一下岗位职责', '2026-10-08 20:05:00');
INSERT INTO `preview_message` VALUES (54, 18, 'ASSISTANT', '当前检索到的《深度工业AI平台员工手册（内部版）》中，没有直接给出某一具体岗位的详细“岗位职责”，但可以根据手册内容归纳出员工在公司日常工作中通常需要遵守和履行的通用职责/规范。可以这样介绍：\n\n---\n\n## 深度工业AI平台员工通用岗位职责\n\n### 1. 假期与考勤相关职责\n- 员工入职满一年后，每年可享受 **10 天带薪年假**。\n- 年假需在本年度内休完，**不可跨年累计**。\n- 法定节假日照常放假，不占用年假额度。\n- 员工应合理规划休假安排，并按照公司制度进行请假、年假申请与考勤确认。\n\n### 2. 费用报销相关职责\n- 员工因公产生的费用，应在费用发生之日起 **7 天内提交报销申请**。\n- 报销单必须附带合规发票，发票抬头必须为公司全称。\n- 住宿费用报销标准为每晚上限 **600 元**，超出部分由员工自理。\n- 员工应确保报销材料真实、合规、完整，并按规定流程提交。\n\n### 3. 餐饮与福利相关职责\n- 公司可为员工提供午餐补贴，标准为每人每天 **30 元**。\n- 晚餐加班超过 **20:00** 的员工，可额外申请夜宵补贴 **20 元**。\n- 每月第一个工作日发放当月全勤奖。\n- 员工应了解公司福利政策，并按制度申请相关补贴或奖励。\n\n### 4. 办公规范相关职责\n- 使用会议室需提前一天在系统预约，单次使用最长 **4 小时**。\n- 内部文档需加密后传输，禁止通过公共网络发送。\n- 离职员工须在最后工作日回交门禁卡与办公设备。\n- 员工应遵守办公资源使用规范，保护公司资产与内部资料安全。\n\n### 5. 信息安全相关职责\n- 密码需每 **90 天强制更换一次**，不得使用连续相同字符。\n- 员工账号若出现异地异常登录，系统会自动锁定并通知管理员。\n- 敏感资料仅限授权人员访问，禁止外带或私自拷贝。\n- 员工有义务配合公司信息安全要求，防止数据泄露、违规传输或越权访问。\n\n---\n\n## 简要概括\n\n如果从制度层面介绍“岗位职责”，可以概括为：\n\n1. **遵守假期与考勤制度**，按时休假、规范请假，确保工作安排与公司制度一致。  \n2. **规范处理费用报销与福利申请**，按要求提交材料，确保合规、及时、真实。  \n3. **履行办公规范与信息安全职责**，正确使用办公设备、会议室、内部文档和公司账号，保护公司资产与敏感数据安全。  \n\n如果您需要的是某个具体岗位，例如“AI算法工程师”“产品经理”“项目实施工程师”“运维工程师”等的岗位职责，当前资料中未包含该部分内容，建议补充岗位说明书、部门职责文件或岗位JD。', '2026-10-08 20:05:18');
INSERT INTO `preview_message` VALUES (55, 19, 'USER', '你好', '2026-10-08 20:19:31');
INSERT INTO `preview_message` VALUES (56, 19, 'ASSISTANT', '您好，我只回答笔记本电脑价格相关问题。请提供型号或品牌。', '2026-10-08 20:19:34');
INSERT INTO `preview_message` VALUES (57, 19, 'USER', 'y9000p多少钱？', '2026-10-08 20:19:51');
INSERT INTO `preview_message` VALUES (58, 19, 'ASSISTANT', '拯救者 Y9000P 2026（i9-14900HX/RTX5060）：13 金砖。', '2026-10-08 20:19:53');
INSERT INTO `preview_message` VALUES (59, 20, 'USER', '????????', '2026-10-12 10:02:34');
INSERT INTO `preview_message` VALUES (60, 20, 'ASSISTANT', '手册未覆盖。', '2026-10-12 10:02:42');
INSERT INTO `preview_message` VALUES (61, 21, 'USER', '介绍一下你的能力', '2026-10-12 10:30:56');
INSERT INTO `preview_message` VALUES (62, 21, 'ASSISTANT', '我只负责回答笔记本电脑价格问题。', '2026-10-12 10:30:59');
INSERT INTO `preview_message` VALUES (63, 22, 'USER', '今天有什么进展可以汇报', '2026-10-12 10:32:14');
INSERT INTO `preview_message` VALUES (64, 22, 'ASSISTANT', '仅可回答笔记本电脑价格相关问题；此问题超出职责范围。', '2026-10-12 10:32:19');
INSERT INTO `preview_message` VALUES (65, 22, 'USER', '推荐一款轻薄本', '2026-10-12 10:33:48');
INSERT INTO `preview_message` VALUES (66, 22, 'ASSISTANT', '无法回答与笔记本价格无关的问题。请提供具体轻薄本型号或规格，我可根据价格资料作答。', '2026-10-12 10:33:56');
INSERT INTO `preview_message` VALUES (67, 22, 'USER', 'ThinkPad X1 多少钱', '2026-10-12 10:34:17');
INSERT INTO `preview_message` VALUES (68, 22, 'ASSISTANT', '手册未覆盖 ThinkPad X1 的价格。', '2026-10-12 10:34:20');
INSERT INTO `preview_message` VALUES (69, 22, 'USER', 'ThinkPad X1 多少钱', '2026-10-12 10:34:35');
INSERT INTO `preview_message` VALUES (70, 22, 'ASSISTANT', '手册未覆盖。', '2026-10-12 10:34:38');
INSERT INTO `preview_message` VALUES (71, 23, 'USER', 'y9000p的价格是多少', '2026-10-13 10:22:05');
INSERT INTO `preview_message` VALUES (72, 23, 'ASSISTANT', '手册未覆盖该机型（Y9000P）的价格，无法回答。', '2026-10-13 10:22:12');
INSERT INTO `preview_message` VALUES (73, 24, 'USER', '??', '2026-10-13 10:25:57');
INSERT INTO `preview_message` VALUES (74, 24, 'ASSISTANT', '抱歉，我只能回答笔记本电脑的价格问题。请询问您想了解的具体笔记本型号或价格。', '2026-10-13 10:26:01');
INSERT INTO `preview_message` VALUES (75, 23, 'USER', 'y9000p的价格是多少', '2026-10-13 10:27:10');
INSERT INTO `preview_message` VALUES (76, 23, 'ASSISTANT', '手册未覆盖，无法给出 y9000p 价格。', '2026-10-13 10:27:13');
INSERT INTO `preview_message` VALUES (77, 23, 'USER', '拯救者价格多少', '2026-10-13 10:27:48');
INSERT INTO `preview_message` VALUES (78, 23, 'ASSISTANT', '手册未覆盖。', '2026-10-13 10:27:51');
INSERT INTO `preview_message` VALUES (79, 25, 'USER', '你好，介绍一下你擅长的领域', '2026-10-13 10:39:28');
INSERT INTO `preview_message` VALUES (80, 25, 'ASSISTANT', '我只负责回答笔记本电脑价格相关问题。你可以直接询问某款笔记本的价格。', '2026-10-13 10:39:33');
INSERT INTO `preview_message` VALUES (81, 23, 'USER', 'y9000p的价格是多少', '2026-10-13 10:40:12');
INSERT INTO `preview_message` VALUES (82, 23, 'ASSISTANT', '手册未覆盖（未提供知识库资料，无法给出 y9000p 的价格）。', '2026-10-13 10:40:15');
INSERT INTO `preview_message` VALUES (83, 23, 'USER', 'y9000p的价格是多少', '2026-10-13 10:45:43');
INSERT INTO `preview_message` VALUES (84, 23, 'ASSISTANT', '手册未覆盖：当前资料未提供 y9000p 的笔记本价格，无法给出价格。', '2026-10-13 10:45:47');
INSERT INTO `preview_message` VALUES (85, 26, 'USER', '你好', '2026-10-13 10:46:02');
INSERT INTO `preview_message` VALUES (86, 26, 'ASSISTANT', '我只负责回答笔记本电脑价格。请提供具体品牌、型号或配置，我可帮您查询价格。', '2026-10-13 10:46:04');
INSERT INTO `preview_message` VALUES (87, 26, 'USER', 'y7000p', '2026-10-13 10:46:11');
INSERT INTO `preview_message` VALUES (88, 26, 'ASSISTANT', '关于 y7000p 的笔记本价格：手册未覆盖。', '2026-10-13 10:46:15');
INSERT INTO `preview_message` VALUES (89, 27, 'USER', '你好', '2026-10-13 10:46:45');
INSERT INTO `preview_message` VALUES (90, 27, 'ASSISTANT', '你好。我只负责笔记本电脑价格查询，请提供具体型号。', '2026-10-13 10:46:49');
INSERT INTO `preview_message` VALUES (91, 27, 'USER', 'y7000p', '2026-10-13 10:46:55');
INSERT INTO `preview_message` VALUES (92, 27, 'ASSISTANT', 'Y7000P 2026（i7-14700H/RTX4060）：8 金砖', '2026-10-13 10:46:59');
INSERT INTO `preview_message` VALUES (93, 27, 'USER', 'y9000p', '2026-10-13 10:47:08');
INSERT INTO `preview_message` VALUES (94, 27, 'ASSISTANT', '13 金砖', '2026-10-13 10:47:12');
INSERT INTO `preview_message` VALUES (95, 27, 'USER', '拯救 y7000', '2026-10-13 10:48:03');
INSERT INTO `preview_message` VALUES (96, 27, 'ASSISTANT', '拯救者 Y7000P 2026（i7-14700H/RTX4060）：8 金砖  \n拯救者 Y7000X 2026（Ultra7/RTX5060）：10 金砖  \n\n若你指的是其他“Y7000”具体型号，手册未覆盖。', '2026-10-13 10:48:09');
INSERT INTO `preview_message` VALUES (97, 28, 'USER', '你好', '2026-10-13 19:20:51');
INSERT INTO `preview_message` VALUES (98, 28, 'ASSISTANT', '抱歉，我只负责回答笔记本电脑价格问题。', '2026-10-13 19:20:56');
INSERT INTO `preview_message` VALUES (99, 28, 'USER', '你好', '2026-10-13 19:23:10');
INSERT INTO `preview_message` VALUES (100, 28, 'ASSISTANT', '抱歉，我只负责回答笔记本电脑价格。', '2026-10-13 19:23:12');
INSERT INTO `preview_message` VALUES (101, 28, 'USER', 'y9000p多少钱', '2026-10-13 19:23:20');
INSERT INTO `preview_message` VALUES (102, 28, 'ASSISTANT', '手册未覆盖', '2026-10-13 19:23:22');
INSERT INTO `preview_message` VALUES (103, 29, 'USER', '你好', '2026-10-13 19:24:55');
INSERT INTO `preview_message` VALUES (104, 29, 'ASSISTANT', '抱歉，我只负责回答与笔记本电脑价格相关的问题。请问您想查询哪款笔记本电脑的价格？', '2026-10-13 19:25:00');
INSERT INTO `preview_message` VALUES (105, 29, 'USER', 'Y9000p的价格是多少？', '2026-10-13 19:25:23');
INSERT INTO `preview_message` VALUES (106, 29, 'ASSISTANT', '手册未覆盖。当前未提供 Y9000p 的价格资料，无法作答。', '2026-10-13 19:25:25');
INSERT INTO `preview_message` VALUES (107, 30, 'USER', '你好', '2026-10-13 19:25:39');
INSERT INTO `preview_message` VALUES (108, 30, 'ASSISTANT', '我只负责笔记本电脑价格。请提供想查询的笔记本品牌或型号。', '2026-10-13 19:25:43');
INSERT INTO `preview_message` VALUES (109, 30, 'USER', 'y9000p的价格是多少', '2026-10-13 19:25:52');
INSERT INTO `preview_message` VALUES (110, 30, 'ASSISTANT', '联想拯救者 Y9000P 2026（i9-14900HX/RTX5060）：13 金砖', '2026-10-13 19:25:55');
INSERT INTO `preview_message` VALUES (111, 31, 'USER', '你好', '2026-10-13 19:32:57');
INSERT INTO `preview_message` VALUES (112, 31, 'ASSISTANT', '您好！我是商店员工，仅负责回答**笔记本电脑价格**相关问题。\n\n请告诉我您想了解哪款笔记本的价格，我来为您查询。', '2026-10-13 19:33:02');
INSERT INTO `preview_message` VALUES (113, 31, 'USER', 'y7000p多少钱', '2026-10-13 19:33:12');
INSERT INTO `preview_message` VALUES (114, 31, 'ASSISTANT', '手册未覆盖', '2026-10-13 19:33:16');
INSERT INTO `preview_message` VALUES (115, 32, 'USER', '?? ????? Y9000P 2024 ?????????', '2026-10-13 19:46:41');
INSERT INTO `preview_message` VALUES (116, 32, 'ASSISTANT', '手册未覆盖。', '2026-10-13 19:46:45');
INSERT INTO `preview_message` VALUES (117, 33, 'USER', '?? ????? Y9000P 2024 ?????????', '2026-10-13 19:46:57');
INSERT INTO `preview_message` VALUES (118, 33, 'ASSISTANT', '手册未覆盖 Y9000P 2024 的价格，不能提供。', '2026-10-13 19:47:04');
INSERT INTO `preview_message` VALUES (119, 34, 'USER', '请问 联想拯救者 Y9000P 2024 的参考价格是多少？', '2026-10-13 19:47:27');
INSERT INTO `preview_message` VALUES (120, 34, 'ASSISTANT', '手册未覆盖，无法提供参考价格。', '2026-10-13 19:47:30');
INSERT INTO `preview_message` VALUES (121, 35, 'USER', '笔记本 3060 显卡大概什么价位？', '2026-10-13 19:47:30');
INSERT INTO `preview_message` VALUES (122, 35, 'ASSISTANT', '手册未覆盖。', '2026-10-13 19:47:35');
INSERT INTO `preview_message` VALUES (123, 36, 'USER', '你好', '2026-10-13 20:38:18');
INSERT INTO `preview_message` VALUES (124, 36, 'ASSISTANT', '您好，我是商店员工，仅负责回答笔记本电脑价格。请问您想了解哪款笔记本的价格？', '2026-10-13 20:38:23');
INSERT INTO `preview_message` VALUES (125, 36, 'USER', '帮我查询一下y7000p的价格', '2026-10-13 20:38:40');
INSERT INTO `preview_message` VALUES (126, 36, 'ASSISTANT', '联想拯救者 Y7000P 2026（i7-14700H/RTX4060）：8 金砖。', '2026-10-13 20:38:45');
INSERT INTO `preview_message` VALUES (127, 37, 'USER', '你好', '2026-10-13 20:41:23');
INSERT INTO `preview_message` VALUES (128, 37, 'ASSISTANT', '你好。我只负责回答笔记本电脑价格，请提供笔记本型号。', '2026-10-13 20:41:28');
INSERT INTO `preview_message` VALUES (129, 37, 'USER', 'y9000p', '2026-10-13 20:41:38');
INSERT INTO `preview_message` VALUES (130, 37, 'ASSISTANT', '拯救者 Y9000P 2026（i9-14900HX/RTX5060）：13 金砖', '2026-10-13 20:41:42');
INSERT INTO `preview_message` VALUES (131, 37, 'USER', '小新', '2026-10-13 20:41:54');
INSERT INTO `preview_message` VALUES (132, 37, 'ASSISTANT', '小新系列笔记本价格：\n\n- 小新 Air 14 2026款（i5-1440P/16G/512G）：4 金砖  \n- 小新 Air 15 2026款（16G/512G）：5 金砖  \n- 小新 Pro 14 2026（Ultra5 336H/32G/1TB）：7 金砖  \n- 小新 Pro 16 2025（酷睿5 220H/24G/1TB）：5 金砖  \n- 小新 16 酷睿Ultra版（16英寸）：6 金砖  \n\n如需指定具体“小新”型号，请补充完整型号。', '2026-10-13 20:42:02');

-- ----------------------------
-- Table structure for skill
-- ----------------------------
DROP TABLE IF EXISTS `skill`;
CREATE TABLE `skill`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Skill 主键',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Skill 名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Skill 描述',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Skill 配置或实现内容',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Skill 状态：ACTIVE 或 DISABLED',
  `version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '1.0' COMMENT 'Skill 版本',
  `created_by` bigint NOT NULL COMMENT '创建管理员 ID，仅用于审计',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of skill
-- ----------------------------
INSERT INTO `skill` VALUES (1, '??????', '??????', '????', 'ACTIVE', '1.0', 1, '2026-10-06 20:01:08', '2026-10-06 20:01:08');
INSERT INTO `skill` VALUES (2, '客服技能', '处理客服咨询', '你是客服助手，请根据知识库回答客户问题。', 'ACTIVE', '1.0', 1, '2026-10-07 20:06:10', '2026-10-07 20:06:10');
INSERT INTO `skill` VALUES (3, '测试', '这是测试用途', '这是一个测试skill，当调用这个skill时，输出“测试skill”', 'ACTIVE', '1.0', 1, '2026-10-08 08:19:42', '2026-10-08 08:19:42');
INSERT INTO `skill` VALUES (4, '测试2', '测试用途', '当用户想要调用“测试2”技能时，就调用此技能，给用户回复“这是一个测试的skill”', 'ACTIVE', '1.0', 1, '2026-10-08 14:36:22', '2026-10-08 14:36:22');
INSERT INTO `skill` VALUES (5, '测试3', '测试用途', '输出“这是一个测试skills”', 'ACTIVE', '1.0', 1, '2026-10-08 14:38:51', '2026-10-08 14:38:51');
INSERT INTO `skill` VALUES (6, '测试4', '测试用途', '当用户想要调用“测试4”技能时，输出“手动测试文档”中的内容', 'ACTIVE', '1.0', 1, '2026-10-08 14:42:29', '2026-10-08 14:42:29');
INSERT INTO `skill` VALUES (7, '????????', '???????????', '????????AI????????,??????????,?????', 'ACTIVE', '1.0', 1, '2026-10-08 16:02:30', '2026-10-08 16:02:30');
INSERT INTO `skill` VALUES (8, '查询笔记本电脑价格', '查询笔记本电脑价格', '能力定位：查询笔记本电脑价格\n能力说明：负责检索「笔记本电脑价格金砖版」知识库并回答售后问题。\n使用场景：询问价格\n约束：只依据关联知识库作答，不编造。', 'ACTIVE', '1.0', 1, '2026-10-08 20:16:34', '2026-10-08 20:16:34');

-- ----------------------------
-- Table structure for skill_knowledge_base
-- ----------------------------
DROP TABLE IF EXISTS `skill_knowledge_base`;
CREATE TABLE `skill_knowledge_base`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联主键',
  `skill_id` bigint NOT NULL COMMENT 'Skill ID',
  `knowledge_base_id` bigint NOT NULL COMMENT '知识库 ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_skill_knowledge_base`(`skill_id` ASC, `knowledge_base_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of skill_knowledge_base
-- ----------------------------
INSERT INTO `skill_knowledge_base` VALUES (1, 1, 7);
INSERT INTO `skill_knowledge_base` VALUES (3, 2, 8);
INSERT INTO `skill_knowledge_base` VALUES (2, 2, 9);
INSERT INTO `skill_knowledge_base` VALUES (4, 3, 12);
INSERT INTO `skill_knowledge_base` VALUES (5, 4, 12);
INSERT INTO `skill_knowledge_base` VALUES (6, 5, 12);
INSERT INTO `skill_knowledge_base` VALUES (7, 6, 12);
INSERT INTO `skill_knowledge_base` VALUES (8, 7, 13);
INSERT INTO `skill_knowledge_base` VALUES (9, 8, 14);

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户主键',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录账号，唯一',
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码哈希（BCrypt）',
  `role` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户角色：ADMIN 或 USER',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户状态：ACTIVE 或 DISABLED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, 'admin', '$2a$10$mk4mpZRSt4D.AyCILm/1MuIVvx9w1g6SfuyQkDmFS76G5Sgs6ajo2', 'ADMIN', 'ACTIVE', '2026-10-06 19:22:51', '2026-10-06 19:22:51');
INSERT INTO `user` VALUES (2, 'chainuser', '$2a$10$mk4mpZRSt4D.AyCILm/1MuIVvx9w1g6SfuyQkDmFS76G5Sgs6ajo2', 'USER', 'ACTIVE', '2026-10-06 20:01:33', '2026-10-06 20:01:33');
INSERT INTO `user` VALUES (3, 'usertest', '$2a$10$2YxWex62SKOkOK5WWnDYRu8u9g7OtJmuvYEdaI.ydZLyGjCRu5hg2', 'USER', 'ACTIVE', '2026-10-08 14:26:26', '2026-10-08 14:26:26');
INSERT INTO `user` VALUES (4, 'e2euser100216', '$2a$10$NaxwjxcR5cpTi7kUnryHF.YlSOAE4sytfDl0COs92iN07WT4WY5MK', 'USER', 'ACTIVE', '2026-10-12 10:02:16', '2026-10-12 10:02:16');
INSERT INTO `user` VALUES (5, 'hong', '$2a$10$nv4a0FdC.UmAHWK4A6rqOetnislbctzE95PUvq9C7x5libivzF5qK', 'USER', 'ACTIVE', '2026-10-12 19:41:43', '2026-10-12 19:41:43');
INSERT INTO `user` VALUES (6, 'permuser102556', '$2a$10$5PW5zj1db6TnRBcGjwKLiOG8Kg56Dx7IQ0ULt/7CncI7XVInQXY0y', 'USER', 'ACTIVE', '2026-10-13 10:25:56', '2026-10-13 10:25:56');
INSERT INTO `user` VALUES (7, 'userA1B2', '$2a$10$SdORBb0WT2usmGufF6IDheGqgFM3KAFZAEzry6Nk64lBMOV4RF5qS', 'USER', 'ACTIVE', '2026-10-13 10:33:23', '2026-10-13 10:33:23');

-- ----------------------------
-- Table structure for user_auth_token
-- ----------------------------
DROP TABLE IF EXISTS `user_auth_token`;
CREATE TABLE `user_auth_token`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '凭证主键',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `token_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '凭证哈希（SHA-256）',
  `expires_at` datetime NOT NULL COMMENT '过期时间',
  `revoked` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已吊销',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_token_hash`(`token_hash` ASC) USING BTREE,
  INDEX `idx_token_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 65 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_auth_token
-- ----------------------------
INSERT INTO `user_auth_token` VALUES (28, 2, 'ecbb880df183df3c3220f9accc2e26f9569cca29efd9162469d33b56bdf2b74d', '2026-10-08 20:36:30', 1, '2026-10-07 20:36:29');
INSERT INTO `user_auth_token` VALUES (41, 3, 'a468cfe21295a5bd7e3481a85dd91f0039c34411101ba054ffb922538aa81001', '2026-10-09 14:26:27', 0, '2026-10-08 14:26:27');
INSERT INTO `user_auth_token` VALUES (47, 4, '4ef46a7760520cad2ed4dbad22163509045f36a4a1bd8e29b12db28c8ba8a578', '2026-10-13 10:02:17', 0, '2026-10-12 10:02:17');
INSERT INTO `user_auth_token` VALUES (55, 1, '74ae1ca80f72ef7bebd75ef409f9ef5f09f426dded1f02204b9c1353cef6f7e7', '2026-10-14 09:57:56', 0, '2026-10-13 09:57:55');
INSERT INTO `user_auth_token` VALUES (56, 1, '91a26606f090dace9ee5b492a81cfded89e8f9702860be7cf3b18c0c88678de1', '2026-10-14 10:20:16', 0, '2026-10-13 10:20:15');
INSERT INTO `user_auth_token` VALUES (57, 1, 'bcb8f5d9214a609c063328118e6064e01092a47b5cc0d46b7865c662b66af88c', '2026-10-14 10:25:56', 0, '2026-10-13 10:25:55');
INSERT INTO `user_auth_token` VALUES (58, 6, '2bbdc7ea8a3a3f66ee6bdbef44235869ceb55132d82d9a32b4c1d5c0e77814cd', '2026-10-14 10:25:57', 0, '2026-10-13 10:25:56');
INSERT INTO `user_auth_token` VALUES (59, 7, '85b93ec5edc5daf293ff7e4d26e46528f64378edc20e3e3c944b60c9acc3b24d', '2026-10-14 10:34:31', 0, '2026-10-13 10:34:31');
INSERT INTO `user_auth_token` VALUES (60, 5, 'eba55c7a412e0c7724b6c1dac866601ed92633b3c5a86d9b556829b2368f2b91', '2026-10-14 19:19:48', 0, '2026-10-13 19:19:47');
INSERT INTO `user_auth_token` VALUES (61, 1, 'a259a2b3ac5031119ab3ed42ece6edbb7d70ce956172d16c3b51f459e71b8778', '2026-10-14 19:46:31', 0, '2026-10-13 19:46:31');
INSERT INTO `user_auth_token` VALUES (62, 1, '3c8230460138a1adff63e7b4bf2952b53fb991370fe1476c5a96ecec5daa217b', '2026-10-14 19:47:26', 0, '2026-10-13 19:47:26');
INSERT INTO `user_auth_token` VALUES (63, 5, '4a8c0e1870aaa7b4ef5328c8277b4d657891968aaa4c1d01b69c56be3773d66a', '2026-10-14 20:41:17', 0, '2026-10-13 20:41:16');
INSERT INTO `user_auth_token` VALUES (64, 5, '2e826ff1a99fa8e16ddb72df2b1fb67b890f25676af1eaf248d2b776659d7ad5', '2026-10-14 20:41:17', 0, '2026-10-13 20:41:16');

SET FOREIGN_KEY_CHECKS = 1;
