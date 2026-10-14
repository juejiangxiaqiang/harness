package com.harness.admin.controller;

import com.harness.admin.domain.knowledgebase.KnowledgeBaseStatus;
import com.harness.admin.service.KnowledgeBaseService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文件说明：管理端预览对话接口集成测试。
 *
 * <p>以 Mock 的 {@link ChatModel} 与 {@link VectorStore} 屏蔽外部模型依赖，
 * 覆盖：管理员对话成功并落库、会话与消息历史查询、未登录/普通用户权限边界、
 * 不存在的会话返回资源不存在。测试环境（provider=local）对话模型被 Mock 覆盖，
 * 避免依赖外部服务。</p>
 *
 * @author harness-admin
 * @since 2026-10-08
 */
@SpringBootTest
@AutoConfigureMockMvc
class PreviewChatControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @MockitoBean
    private ChatModel chatModel;

    @MockitoBean
    private VectorStore vectorStore;

    /**
     * 验证管理员对话成功：回复返回、会话与消息落库、历史可查询。
     */
    @Test
    void chatAndHistory_shouldSucceedForAdmin() throws Exception {
        when(chatModel.call(anyString())).thenReturn("模拟回复");
        when(vectorStore.similaritySearch(any(org.springframework.ai.vectorstore.SearchRequest.class)))
                .thenReturn(List.of(new Document("知识库片段")));
        long employeeId = createEmployee("预览对话员工", "预览对话资料");
        String token = loginToken("admin", "Admin@12345");

        // 发起对话（新会话）
        MvcResult chatResult = mockMvc.perform(post("/api/digital-employees/" + employeeId + "/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"售后流程是什么？\"}")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String chatBody = chatResult.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(chatBody, "$.code")).isEqualTo("SUCCESS");
        assertThat(JsonPath.<String>read(chatBody, "$.data.reply")).isEqualTo("模拟回复");
        long conversationId = ((Number) JsonPath.read(chatBody, "$.data.conversationId")).longValue();

        // 会话历史包含刚创建的会话
        MvcResult convResult = mockMvc.perform(get("/api/digital-employees/" + employeeId + "/conversations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String convBody = convResult.getResponse().getContentAsString();
        assertThat(((Number) JsonPath.read(convBody, "$.data.total")).longValue()).isEqualTo(1);

        // 消息历史包含用户问题与助手回复
        MvcResult msgResult = mockMvc.perform(get("/api/digital-employees/" + employeeId + "/conversations/"
                        + conversationId + "/messages").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String msgBody = msgResult.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(msgBody, "$.data[0].role")).isEqualTo("USER");
        assertThat(JsonPath.<String>read(msgBody, "$.data[1].role")).isEqualTo("ASSISTANT");
    }

    /**
     * 验证未登录被拒；普通用户仅可与已发布数字员工对话。
     */
    @Test
    void chat_forUser_shouldAllowPublishedOnly() throws Exception {
        when(chatModel.call(anyString())).thenReturn("模拟回复");
        when(vectorStore.similaritySearch(any(org.springframework.ai.vectorstore.SearchRequest.class)))
                .thenReturn(List.of(new Document("知识库片段")));
        // 未登录访问：AUTH_UNAUTHORIZED
        MvcResult anonymous = mockMvc.perform(post("/api/digital-employees/1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"你好\"}"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(JsonPath.<String>read(anonymous.getResponse().getContentAsString(), "$.code"))
                .isEqualTo("AUTH_UNAUTHORIZED");

        // 已发布员工：普通用户可对话
        long publishedId = createEmployee("用户可见员工", "可见资料");
        publishEmployee(publishedId);
        // 未发布员工：普通用户不可见
        long draftId = createEmployee("用户不可见员工", "不可见资料");

        registerUser("chatuser", "password123");
        String userToken = loginToken("chatuser", "password123");
        MvcResult allowed = mockMvc.perform(post("/api/digital-employees/" + publishedId + "/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"你好\"}")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(JsonPath.<String>read(allowed.getResponse().getContentAsString(), "$.code"))
                .isEqualTo("SUCCESS");

        MvcResult forbidden = mockMvc.perform(post("/api/digital-employees/" + draftId + "/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"你好\"}")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(JsonPath.<String>read(forbidden.getResponse().getContentAsString(), "$.code"))
                .isEqualTo("RESOURCE_NOT_FOUND");
    }

    /**
     * 管理员发布数字员工。
     *
     * @param employeeId 数字员工 ID
     */
    private void publishEmployee(long employeeId) throws Exception {
        String token = loginToken("admin", "Admin@12345");
        mockMvc.perform(post("/api/digital-employees/" + employeeId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    /**
     * 验证查询不存在的会话返回资源不存在。
     */
    @Test
    void listMessages_withMissingConversation_shouldReturnNotFound() throws Exception {
        long employeeId = createEmployee("历史员工", "历史资料");
        String token = loginToken("admin", "Admin@12345");

        MvcResult result = mockMvc.perform(get("/api/digital-employees/" + employeeId + "/conversations/999999/messages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(JsonPath.<String>read(result.getResponse().getContentAsString(), "$.code"))
                .isEqualTo("RESOURCE_NOT_FOUND");
    }

    /**
     * 创建 READY 知识库、可用 Skill 与数字员工，返回数字员工 ID。
     */
    private long createEmployee(String name, String kbContent) throws Exception {
        long kbId = uploadAndAwait("chat-资料.txt", kbContent, name + "库", KnowledgeBaseStatus.READY);
        String token = loginToken("admin", "Admin@12345");
        String skillBody = "{\"name\":\"" + name + "技能\",\"description\":\"\",\"content\":\"能力配置\","
                + "\"knowledgeBaseIds\":[" + kbId + "]}";
        MvcResult skillResult = mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(skillBody)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        long skillId = ((Number) JsonPath.read(skillResult.getResponse().getContentAsString(), "$.data.id")).longValue();
        String employeeBody = "{\"name\":\"" + name + "\",\"description\":\"\",\"systemPrompt\":\"你是客服助手\","
                + "\"skillIds\":[" + skillId + "],\"modelConfig\":{\"model\":\"m\"}}";
        MvcResult employeeResult = mockMvc.perform(post("/api/digital-employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeBody)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return ((Number) JsonPath.read(employeeResult.getResponse().getContentAsString(), "$.data.id")).longValue();
    }

    /**
     * 上传知识库并等待 READY。
     */
    private long uploadAndAwait(String fileName, String content, String name, KnowledgeBaseStatus expected)
            throws Exception {
        String token = loginToken("admin", "Admin@12345");
        MockMultipartFile file = new MockMultipartFile("file", fileName, "text/plain",
                content.getBytes(StandardCharsets.UTF_8));
        MvcResult result = mockMvc.perform(multipart("/api/knowledge-bases")
                        .file(file)
                        .param("name", name)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        long kbId = ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.data.id")).longValue();
        KnowledgeBaseStatus current = knowledgeBaseService.detail(kbId).status();
        for (int i = 0; i < 60 && current != expected; i++) {
            Thread.sleep(200);
            current = knowledgeBaseService.detail(kbId).status();
        }
        return kbId;
    }

    /**
     * 注册测试用户。
     */
    private void registerUser(String username, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk());
    }

    /**
     * 登录并提取凭证。
     */
    private String loginToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.<String>read(result.getResponse().getContentAsString(), "$.data.token");
    }
}