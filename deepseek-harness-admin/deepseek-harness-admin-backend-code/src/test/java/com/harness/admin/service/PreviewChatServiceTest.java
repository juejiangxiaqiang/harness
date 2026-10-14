package com.harness.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.harness.admin.domain.common.BizException;
import com.harness.admin.domain.common.ErrorCode;
import com.harness.admin.domain.common.PageResult;
import com.harness.admin.domain.digitalemployee.DigitalEmployeeResponse;
import com.harness.admin.domain.digitalemployee.DigitalEmployeeStatus;
import com.harness.admin.domain.previewchat.ChatRequest;
import com.harness.admin.domain.previewchat.ChatResponse;
import com.harness.admin.domain.previewchat.ConversationResponse;
import com.harness.admin.domain.previewchat.MessageResponse;
import com.harness.admin.domain.user.AuthContext;
import com.harness.admin.domain.user.UserPrincipal;
import com.harness.admin.domain.user.UserRole;
import com.harness.admin.mapper.PreviewConversationMapper;
import com.harness.admin.mapper.PreviewMessageMapper;
import com.harness.admin.mapper.entity.PreviewConversation;
import com.harness.admin.mapper.entity.PreviewMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 文件说明：管理端预览对话服务单元测试。
 *
 * <p>覆盖：对话生成与落库（发起/延续会话）、对话模型未配置错误路径、
 * 知识库检索、会话历史分页、消息历史查询与越权会话拒绝。</p>
 *
 * @author harness-admin
 * @since 2026-10-08
 */
@ExtendWith(MockitoExtension.class)
class PreviewChatServiceTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private DigitalEmployeeService employeeService;

    @Mock
    private ReleaseSnapshotService snapshotService;

    @Mock
    private PreviewConversationMapper conversationMapper;

    @Mock
    private PreviewMessageMapper messageMapper;

    @SuppressWarnings("unchecked")
    private final ObjectProvider<ChatModel> chatModelProvider = mock(ObjectProvider.class);

    private PreviewChatService service;

    @BeforeEach
    void setUp() {
        service = new PreviewChatService(chatModelProvider, vectorStore, employeeService,
                snapshotService, conversationMapper, messageMapper);
        AuthContext.set(new UserPrincipal(1L, "admin", UserRole.ADMIN));
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void chat_createsConversationAndPersistsMessages() {
        when(chatModelProvider.getIfAvailable()).thenReturn(chatModel);
        when(employeeService.detail(2L)).thenReturn(employeeResponse());
        when(snapshotService.loadSkillSnapshots(2L)).thenReturn(List.of(Map.of("skillId", 3L, "skillVersion", "1")));
        when(snapshotService.expandKnowledgeBases(any())).thenReturn(List.of(Map.of("knowledgeBaseId", 5L)));
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(new Document("客服知识库片段")));
        when(chatModel.call(anyString())).thenReturn("这是回复");
        // 模拟 MyBatis-Plus 在 insert 后回填自增主键
        when(conversationMapper.insert(any(PreviewConversation.class))).thenAnswer(inv -> {
            ((PreviewConversation) inv.getArgument(0)).setId(1L);
            return 1;
        });

        ChatResponse response = service.chat(2L, new ChatRequest("售后流程是什么？", null));

        assertThat(response.reply()).isEqualTo("这是回复");
        assertThat(response.conversationId()).isNotNull();
        // 核心回归：检索必须按知识库传入 kb_id 过滤条件（不再依赖召回元数据），
        // 防止再次退回「mock 返回带 kb_id 的 Document」掩盖 schema 未注册的缺陷
        ArgumentCaptor<SearchRequest> searchCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(searchCaptor.capture());
        assertThat(searchCaptor.getValue().getFilterExpression()).isNotNull();
        assertThat(searchCaptor.getValue().getFilterExpression().toString()).contains("kb_id");
        ArgumentCaptor<PreviewConversation> captor = ArgumentCaptor.forClass(PreviewConversation.class);
        verify(conversationMapper).insert(captor.capture());
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(1L);
        // 问题与回复各落库一次，角色分别为 USER 与 ASSISTANT
        ArgumentCaptor<PreviewMessage> messageCaptor = ArgumentCaptor.forClass(PreviewMessage.class);
        verify(messageMapper, org.mockito.Mockito.times(2)).insert(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues())
                .extracting(PreviewMessage::getRole)
                .containsExactlyInAnyOrder("USER", "ASSISTANT");
    }

    @Test
    void chat_withoutChatModel_shouldRejectNotConfigured() {
        when(chatModelProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> service.chat(2L, new ChatRequest("问题", null)))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHAT_PROVIDER_NOT_CONFIGURED);
        verify(conversationMapper, never()).insert(any(PreviewConversation.class));
        verify(messageMapper, never()).insert(any(PreviewMessage.class));
    }

    @Test
    void chat_withGenerationFailure_shouldRejectChatFailed() {
        when(chatModelProvider.getIfAvailable()).thenReturn(chatModel);
        when(employeeService.detail(2L)).thenReturn(employeeResponse());
        when(snapshotService.loadSkillSnapshots(2L)).thenReturn(List.of(Map.of("skillId", 3L, "skillVersion", "1")));
        when(snapshotService.expandKnowledgeBases(any())).thenReturn(List.of(Map.of("knowledgeBaseId", 5L)));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(chatModel.call(anyString())).thenThrow(new RuntimeException("provider down"));

        assertThatThrownBy(() -> service.chat(2L, new ChatRequest("问题", null)))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHAT_FAILED);
    }

    @Test
    void chat_reusesExistingConversation() {
        when(chatModelProvider.getIfAvailable()).thenReturn(chatModel);
        when(employeeService.detail(2L)).thenReturn(employeeResponse());
        when(snapshotService.loadSkillSnapshots(2L)).thenReturn(List.of(Map.of("skillId", 3L, "skillVersion", "1")));
        when(snapshotService.expandKnowledgeBases(any())).thenReturn(List.of(Map.of("knowledgeBaseId", 5L)));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(chatModel.call(anyString())).thenReturn("回复");
        PreviewConversation existing = conversation(8L, 2L);
        when(conversationMapper.selectById(8L)).thenReturn(existing);

        service.chat(2L, new ChatRequest("继续", 8L));

        // 复用会话，不新建
        verify(conversationMapper, never()).insert(any(PreviewConversation.class));
        verify(conversationMapper).updateById(existing);
    }

    @Test
    void chat_withForeignConversation_shouldRejectResourceNotFound() {
        when(chatModelProvider.getIfAvailable()).thenReturn(chatModel);
        when(employeeService.detail(2L)).thenReturn(employeeResponse());
        // 会话属于其它数字员工
        when(conversationMapper.selectById(8L)).thenReturn(conversation(8L, 99L));

        assertThatThrownBy(() -> service.chat(2L, new ChatRequest("继续", 8L)))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void listConversations_shouldReturnPagedWithCountAndLastMessage() {
        PreviewConversation conversation = conversation(8L, 2L);
        Page<PreviewConversation> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(conversation));
        when(conversationMapper.selectPage(any(), any())).thenReturn(page);
        when(messageMapper.selectCount(any())).thenReturn(2L);
        PreviewMessage latest = new PreviewMessage();
        latest.setContent("最后消息");
        when(messageMapper.selectList(any())).thenReturn(List.of(latest));

        PageResult<ConversationResponse> result = service.listConversations(2L, 1, 10);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).messageCount()).isEqualTo(2L);
        assertThat(result.items().get(0).lastMessage()).isEqualTo("最后消息");
    }

    @Test
    void listMessages_shouldReturnOrdered() {
        PreviewConversation conversation = conversation(8L, 2L);
        when(conversationMapper.selectById(8L)).thenReturn(conversation);
        PreviewMessage userMsg = message(1L, "USER", "问题");
        PreviewMessage assistantMsg = message(2L, "ASSISTANT", "回复");
        when(messageMapper.selectList(any())).thenReturn(List.of(userMsg, assistantMsg));

        List<MessageResponse> messages = service.listMessages(2L, 8L);

        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).role()).isEqualTo("USER");
        assertThat(messages.get(1).content()).isEqualTo("回复");
    }

    @Test
    void listMessages_withForeignConversation_shouldRejectNotFound() {
        when(conversationMapper.selectById(8L)).thenReturn(conversation(8L, 99L));

        assertThatThrownBy(() -> service.listMessages(2L, 8L))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    /**
     * 构造数字员工响应。
     */
    private DigitalEmployeeResponse employeeResponse() {
        return new DigitalEmployeeResponse(2L, "售后助手", "描述", "你是客服助手",
                Map.of("model", "configured-model"), DigitalEmployeeStatus.PUBLISHED, "1",
                List.of(3L), LocalDateTime.now());
    }

    private PreviewConversation conversation(Long id, Long employeeId) {
        PreviewConversation c = new PreviewConversation();
        c.setId(id);
        c.setDigitalEmployeeId(employeeId);
        c.setCreatedAt(LocalDateTime.now());
        return c;
    }

    private PreviewMessage message(Long id, String role, String content) {
        PreviewMessage m = new PreviewMessage();
        m.setId(id);
        m.setRole(role);
        m.setContent(content);
        return m;
    }
}