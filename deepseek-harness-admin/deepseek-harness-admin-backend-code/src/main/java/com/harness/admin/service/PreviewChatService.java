package com.harness.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.harness.admin.domain.common.BizException;
import com.harness.admin.domain.common.ErrorCode;
import com.harness.admin.domain.common.PageResult;
import com.harness.admin.domain.digitalemployee.DigitalEmployeeResponse;
import com.harness.admin.domain.previewchat.ChatRequest;
import com.harness.admin.domain.previewchat.ChatResponse;
import com.harness.admin.domain.previewchat.ConversationResponse;
import com.harness.admin.domain.previewchat.MessageResponse;
import com.harness.admin.domain.user.AuthContext;
import com.harness.admin.domain.user.UserPrincipal;
import com.harness.admin.mapper.PreviewConversationMapper;
import com.harness.admin.mapper.PreviewMessageMapper;
import com.harness.admin.mapper.entity.PreviewConversation;
import com.harness.admin.mapper.entity.PreviewMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 文件说明：管理端预览对话服务。
 *
 * <p>职责：在数字员工预览页为管理员提供实时对话验证。发起/延续会话、
 * 按数字员工当前配置（系统提示词 + 关联 READY 知识库）检索并生成回复、
 * 将用户问题与回复落库，并提供会话与消息历史查询。</p>
 *
 * <p>约束：对话仅限管理员（控制器鉴权）；回复一次性返回（非流式）；
 * 会话/消息持久化于 MySQL；对话内容不进入审计日志。</p>
 *
 * @author harness-admin
 * @since 2026-10-08
 */
@Service
public class PreviewChatService {

    /** 结构化日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(PreviewChatService.class);

    /** 单页大小上限 */
    private static final long MAX_PAGE_SIZE = 100;

    /** 一条对话检索的片段上限 */
    private static final int TOP_K = 5;

    /** 会话标题取首条问题的最大长度 */
    private static final int TITLE_MAX = 20;

    /** 消息角色：用户 */
    private static final String ROLE_USER = "USER";

    /** 消息角色：数字员工 */
    private static final String ROLE_ASSISTANT = "ASSISTANT";

    /** 聊天模型（可空，配置 OpenAI 兼容 provider 后才存在） */
    private final ObjectProvider<ChatModel> chatModelProvider;

    /** 向量存储（RAG 检索） */
    private final VectorStore vectorStore;

    /** 数字员工服务（读取当前配置） */
    private final DigitalEmployeeService employeeService;

    /** 发布快照服务（展开 Skills 关联知识库） */
    private final ReleaseSnapshotService snapshotService;

    /** 会话表数据访问 */
    private final PreviewConversationMapper conversationMapper;

    /** 消息表数据访问 */
    private final PreviewMessageMapper messageMapper;

    /**
     * 构造预览对话服务。
     *
     * @param chatModelProvider 聊天模型提供者
     * @param vectorStore       向量存储
     * @param employeeService   数字员工服务
     * @param snapshotService   发布快照服务
     * @param conversationMapper 会话表数据访问
     * @param messageMapper     消息表数据访问
     */
    public PreviewChatService(ObjectProvider<ChatModel> chatModelProvider, VectorStore vectorStore,
                              DigitalEmployeeService employeeService, ReleaseSnapshotService snapshotService,
                              PreviewConversationMapper conversationMapper, PreviewMessageMapper messageMapper) {
        this.chatModelProvider = chatModelProvider;
        this.vectorStore = vectorStore;
        this.employeeService = employeeService;
        this.snapshotService = snapshotService;
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    /**
     * 发起或延续会话并生成回复，将问题与回复持久化。
     *
     * @param employeeId 数字员工 ID
     * @param request    对话请求
     * @return 会话 ID 与回复
     */
    @Transactional
    public ChatResponse chat(Long employeeId, ChatRequest request) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        // 未配置对话模型（如 SPRING_AI_CHAT_PROVIDER 非 openai）时返回明确错误，避免外部依赖缺失时的空指针
        if (chatModel == null) {
            throw new BizException(ErrorCode.CHAT_PROVIDER_NOT_CONFIGURED, "对话模型未配置，请检查环境配置");
        }
        UserPrincipal operator = AuthContext.get();
        DigitalEmployeeResponse employee = employeeService.detail(employeeId);
        PreviewConversation conversation = resolveConversation(employeeId, operator, request);
        insertMessage(conversation.getId(), ROLE_USER, request.message());
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);

        List<Long> kbIds = resolveKnowledgeBaseIds(employeeId);
        List<String> chunks = retrieve(kbIds, request.message());
        String prompt = buildPrompt(employee.systemPrompt(), chunks, request.message());
        String reply;
        try {
            reply = chatModel.call(prompt).trim();
        } catch (RuntimeException ex) {
            log.warn("preview chat generation failed, employeeId={}, message=:{}", employeeId, ex.getMessage());
            throw new BizException(ErrorCode.CHAT_FAILED, "对话生成失败，请稍后重试");
        }
        insertMessage(conversation.getId(), ROLE_ASSISTANT, reply);
        log.info("preview chat completed, employeeId={}, conversationId={}, createdBy={}",
                employeeId, conversation.getId(), operator == null ? null : operator.id());
        return new ChatResponse(conversation.getId(), reply, LocalDateTime.now());
    }

    /**
     * 分页查询某数字员工的会话历史（倒序）。
     *
     * @param employeeId 数字员工 ID
     * @param page       页码
     * @param size       每页条数
     * @return 会话分页结果
     */
    public PageResult<ConversationResponse> listConversations(Long employeeId, long page, long size) {
        // 校验数字员工存在且对当前角色可见（普通用户仅可访问已发布数字员工）
        employeeService.detail(employeeId);
        long safePage = Math.max(page, 1);
        long safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<PreviewConversation> result = conversationMapper.selectPage(
                new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<PreviewConversation>()
                        .eq(PreviewConversation::getDigitalEmployeeId, employeeId)
                        .orderByDesc(PreviewConversation::getCreatedAt));
        List<ConversationResponse> items = new ArrayList<>();
        for (PreviewConversation conversation : result.getRecords()) {
            items.add(toConversationResponse(conversation));
        }
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), items);
    }

    /**
     * 查询某数字员工指定会话的全部消息（正序）。
     *
     * @param employeeId     数字员工 ID
     * @param conversationId 会话 ID
     * @return 消息列表
     */
    public List<MessageResponse> listMessages(Long employeeId, Long conversationId) {
        // 校验数字员工存在且对当前角色可见（普通用户仅可访问已发布数字员工）
        employeeService.detail(employeeId);
        PreviewConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !conversation.getDigitalEmployeeId().equals(employeeId)) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "会话不存在");
        }
        return messageMapper.selectList(new LambdaQueryWrapper<PreviewMessage>()
                        .eq(PreviewMessage::getConversationId, conversationId)
                        .orderByAsc(PreviewMessage::getId))
                .stream().map(m -> new MessageResponse(m.getId(), m.getRole(), m.getContent(), m.getCreatedAt()))
                .toList();
    }

    /**
     * 解析或创建会话：存在 conversationId 时校验归属，否则新建。
     *
     * @param employeeId 数字员工 ID
     * @param operator   当前操作者
     * @param request    对话请求
     * @return 会话实体
     */
    private PreviewConversation resolveConversation(Long employeeId, UserPrincipal operator, ChatRequest request) {
        if (request.conversationId() != null) {
            PreviewConversation conversation = conversationMapper.selectById(request.conversationId());
            if (conversation == null || !conversation.getDigitalEmployeeId().equals(employeeId)) {
                throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "会话不存在");
            }
            return conversation;
        }
        PreviewConversation conversation = new PreviewConversation();
        conversation.setDigitalEmployeeId(employeeId);
        // 标题取首条问题的摘要，便于历史列表识别
        String message = request.message().trim();
        conversation.setTitle(message.length() > TITLE_MAX ? message.substring(0, TITLE_MAX) : message);
        conversation.setCreatedBy(operator == null ? null : operator.id());
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.insert(conversation);
        return conversation;
    }

    /**
     * 展开数字员工关联 Skills 下的 READY 知识库 ID 集合。
     *
     * @param employeeId 数字员工 ID
     * @return 知识库 ID 列表
     */
    private List<Long> resolveKnowledgeBaseIds(Long employeeId) {
        // 复用发布快照的展开逻辑：校验 Skill 可用且知识库 READY
        List<Map<String, Object>> skills = snapshotService.loadSkillSnapshots(employeeId);
        List<Map<String, Object>> bases = snapshotService.expandKnowledgeBases(skills);
        return bases.stream().map(b -> (Long) b.get("knowledgeBaseId")).toList();
    }

    /**
     * 基于用户问题按知识库分段检索文档片段。
     *
     * <p>意图：默认 RedisVectorStore 的召回元数据仅含已注册的 {@code metadataFields}，
     * 若 {@code kb_id} 未注册进 schema，召回结果不会携带 kb_id，内存过滤将恒为空，
     * 导致数字员工检索不到自身知识库。因此改为「逐知识库检索」：为每个目标库构造
     * {@code filterExpression(kb_id==kbId)} 的相似度检索，服务端即按库圈定结果；
     * 各库命中片段汇总后截取 {@link #TOP_K} 条。</p>
     *
     * <p>前提约束：必须在 RedisVectorStore 配置中注册 {@code kb_id} 为 NUMERIC 元数据字段，
     * 且存量索引已执行 {@code FT.ALTERINDEX ... SCHEMA ADD $.kb_id AS kb_id NUMERIC}，
     * 否则该过滤条件无效（见 {@code SpringAiConfig#redisVectorStore}）。</p>
     *
     * @param kbIds   数字员工可用的知识库 ID 集合
     * @param message 用户问题
     * @return 命中的文档片段内容（归属目标知识库）
     */
    private List<String> retrieve(List<Long> kbIds, String message) {
        List<String> hits = new ArrayList<>();
        for (Long kbId : kbIds) {
            if (hits.size() >= TOP_K) {
                break;
            }
            // 按库过滤的相似度检索：服务端只返回该知识库的命中，避免跨库污染
            Filter.Expression kbFilter = new FilterExpressionBuilder()
                    .eq(VectorIndexService.METADATA_KB_ID, kbId).build();
            List<Document> documents = vectorStore.similaritySearch(
                    SearchRequest.builder().query(message).topK(TOP_K).filterExpression(kbFilter).build());
            for (Document document : documents) {
                hits.add(document.getText());
                if (hits.size() >= TOP_K) {
                    break;
                }
            }
        }
        return hits;
    }

    /**
     * 组装对话提示词：系统提示词 + 检索片段 + 用户问题。
     *
     * @param systemPrompt 系统提示词
     * @param chunks       检索片段
     * @param message      用户问题
     * @return 提示词
     */
    private String buildPrompt(String systemPrompt, List<String> chunks, String message) {
        StringBuilder prompt = new StringBuilder(systemPrompt == null ? "" : systemPrompt);
        if (!chunks.isEmpty()) {
            prompt.append("\n\n以下是检索到的知识库资料，请优先依据其中内容作答：\n");
            prompt.append(String.join("\n", chunks));
        }
        prompt.append("\n\n用户问题：\n").append(message);
        return prompt.toString();
    }

    /**
     * 持久化一条消息。
     *
     * @param conversationId 会话 ID
     * @param role           消息角色
     * @param content        消息内容
     */
    private void insertMessage(Long conversationId, String role, String content) {
        PreviewMessage message = new PreviewMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
    }

    /**
     * 会话实体转响应模型（补充消息数与最近消息摘要）。
     *
     * @param conversation 会话实体
     * @return 会话响应
     */
    private ConversationResponse toConversationResponse(PreviewConversation conversation) {
        Long count = messageMapper.selectCount(new LambdaQueryWrapper<PreviewMessage>()
                .eq(PreviewMessage::getConversationId, conversation.getId()));
        List<PreviewMessage> latest = messageMapper.selectList(new LambdaQueryWrapper<PreviewMessage>()
                .eq(PreviewMessage::getConversationId, conversation.getId())
                .orderByDesc(PreviewMessage::getId).last("limit 1"));
        String lastMessage = latest.isEmpty() ? "" : latest.get(0).getContent();
        return new ConversationResponse(conversation.getId(), conversation.getDigitalEmployeeId(),
                conversation.getTitle(), conversation.getCreatedAt(), count, lastMessage);
    }
}