package com.harness.admin.controller;

import com.harness.admin.domain.knowledgebase.KnowledgeBaseResponse;
import com.harness.admin.domain.knowledgebase.KnowledgeBaseStatus;
import com.harness.admin.service.KnowledgeBaseService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文件说明：数字员工接口集成测试。
 *
 * <p>覆盖管理员创建（Skill 关联、模型配置）、请求体忽略知识库 ID、
 * 未登录/普通用户权限边界、列表与详情。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@SpringBootTest
@AutoConfigureMockMvc
class DigitalEmployeeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    /**
     * 验证管理员创建数字员工成功，且请求体中的知识库 ID 被忽略。
     */
    @Test
    void create_withSkill_shouldSucceedAndIgnoreKnowledgeBaseIds() throws Exception {
        long skillId = createReadySkill("客服知识库", "doc.txt", "客服资料");
        String token = loginToken("admin", "Admin@12345");
        // 请求体携带 knowledgeBaseIds 字段，服务端应忽略（数字员工不直接关联知识库）
        String body = "{\"name\":\"售后助手\",\"description\":\"处理售后咨询\",\"systemPrompt\":\"你是售后服务助手\","
                + "\"skillIds\":[" + skillId + "],\"modelConfig\":{\"model\":\"configured-model\"},"
                + "\"knowledgeBaseIds\":[999999]}";

        MvcResult result = mockMvc.perform(post("/api/digital-employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(content, "$.code")).isEqualTo("SUCCESS");
        assertThat(JsonPath.<String>read(content, "$.data.status")).isEqualTo("DRAFT");
        assertThat(((Number) JsonPath.read(content, "$.data.skillIds[0]")).longValue()).isEqualTo(skillId);
        assertThat(JsonPath.<String>read(content, "$.data.modelConfig.model")).isEqualTo("configured-model");
    }

    /**
     * 验证关联不存在的 Skill 时创建被拒绝。
     */
    @Test
    void create_withMissingSkill_shouldReject() throws Exception {
        String token = loginToken("admin", "Admin@12345");
        String body = "{\"name\":\"助手\",\"systemPrompt\":\"提示词\",\"skillIds\":[999999],"
                + "\"modelConfig\":{\"model\":\"m\"}}";

        MvcResult result = mockMvc.perform(post("/api/digital-employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(content, "$.code")).isEqualTo("RESOURCE_NOT_FOUND");
    }

    /**
     * 验证未登录与普通用户访问被拒绝。
     */
    @Test
    void create_withoutAdmin_shouldReject() throws Exception {
        MvcResult anonymous = mockMvc.perform(post("/api/digital-employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"助手\",\"systemPrompt\":\"提示词\",\"skillIds\":[1]}"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(JsonPath.<String>read(anonymous.getResponse().getContentAsString(), "$.code"))
                .isEqualTo("AUTH_UNAUTHORIZED");

        registerUser("employeeuser", "password123");
        String userToken = loginToken("employeeuser", "password123");
        MvcResult forbidden = mockMvc.perform(post("/api/digital-employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"助手\",\"systemPrompt\":\"提示词\",\"skillIds\":[1]}")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(JsonPath.<String>read(forbidden.getResponse().getContentAsString(), "$.code"))
                .isEqualTo("RESOURCE_FORBIDDEN");
    }

    /**
     * 验证列表与详情查询。
     */
    @Test
    void listAndDetail_shouldReturnEmployees() throws Exception {
        long skillId = createReadySkill("列表知识库", "doc.txt", "列表资料");
        String token = loginToken("admin", "Admin@12345");
        String body = "{\"name\":\"咨询助手\",\"description\":\"咨询\",\"systemPrompt\":\"提示词\","
                + "\"skillIds\":[" + skillId + "],\"modelConfig\":{\"model\":\"m\"}}";
        MvcResult created = mockMvc.perform(post("/api/digital-employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        long employeeId = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.data.id")).longValue();

        MvcResult listResult = mockMvc.perform(get("/api/digital-employees").param("page", "1").param("size", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(JsonPath.<String>read(listResult.getResponse().getContentAsString(), "$.code")).isEqualTo("SUCCESS");

        MvcResult detailResult = mockMvc.perform(get("/api/digital-employees/" + employeeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(JsonPath.<String>read(detailResult.getResponse().getContentAsString(), "$.data.name"))
                .isEqualTo("咨询助手");
    }

    /**
     * 验证普通用户列表/详情：仅可见已发布数字员工，且内部配置字段被裁剪。
     */
    @Test
    void user_listAndDetail_shouldSeePublishedOnly() throws Exception {
        long skillId = createReadySkill("权限知识库", "doc.txt", "权限资料");
        String adminToken = loginToken("admin", "Admin@12345");
        // 员工 A：发布；员工 B：保持草稿
        long publishedId = createEmployee(adminToken, skillId, "用户可见员工");
        long draftId = createEmployee(adminToken, skillId, "用户不可见员工");
        mockMvc.perform(post("/api/digital-employees/" + publishedId + "/publish")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        registerUser("listuser", "password123");
        String userToken = loginToken("listuser", "password123");

        // 列表：仅含已发布员工，不含草稿；响应裁剪 systemPrompt/modelConfig/skillIds
        MvcResult listResult = mockMvc.perform(get("/api/digital-employees").param("page", "1").param("size", "100")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();
        String listBody = listResult.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(listBody, "$.code")).isEqualTo("SUCCESS");
        assertThat(JsonPath.<List<?>>read(listBody, "$.data.items[?(@.name=='用户可见员工')]")).hasSize(1);
        assertThat(JsonPath.<List<?>>read(listBody, "$.data.items[?(@.name=='用户不可见员工')]")).isEmpty();
        // 已发布员工响应裁剪内部配置字段
        List<?> visibleItems = JsonPath.read(listBody, "$.data.items[?(@.name=='用户可见员工')]");
        @SuppressWarnings("unchecked")
        Map<String, Object> visibleItem = (Map<String, Object>) ((List<?>) visibleItems).get(0);
        assertThat(visibleItem.get("systemPrompt")).isNull();
        assertThat((List<?>) visibleItem.get("skillIds")).isEmpty();

        // 详情：已发布员工可查看且裁剪；草稿员工返回资源不存在
        MvcResult detailOk = mockMvc.perform(get("/api/digital-employees/" + publishedId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();
        String detailOkBody = detailOk.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(detailOkBody, "$.code")).isEqualTo("SUCCESS");
        assertThat(JsonPath.<String>read(detailOkBody, "$.data.name")).isEqualTo("用户可见员工");
        assertThat(JsonPath.<String>read(detailOkBody, "$.data.systemPrompt")).isNull();

        MvcResult detailDraft = mockMvc.perform(get("/api/digital-employees/" + draftId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(JsonPath.<String>read(detailDraft.getResponse().getContentAsString(), "$.code"))
                .isEqualTo("RESOURCE_NOT_FOUND");
    }

    /**
     * 创建数字员工并返回 ID。
     *
     * @param token   管理员凭证
     * @param skillId 关联 Skill ID
     * @param name    员工名称
     * @return 数字员工 ID
     */
    private long createEmployee(String token, long skillId, String name) throws Exception {
        String body = "{\"name\":\"" + name + "\",\"description\":\"\",\"systemPrompt\":\"提示词\","
                + "\"skillIds\":[" + skillId + "],\"modelConfig\":{\"model\":\"m\"}}";
        MvcResult result = mockMvc.perform(post("/api/digital-employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.data.id")).longValue();
    }

    /**
     * 创建 READY 知识库并创建可用 Skill，返回 Skill ID。
     */
    private long createReadySkill(String kbName, String fileName, String content) throws Exception {
        long kbId = uploadAndAwait(fileName, content, kbName, KnowledgeBaseStatus.READY);
        String token = loginToken("admin", "Admin@12345");
        String body = "{\"name\":\"" + kbName + "技能\",\"description\":\"\",\"content\":\"能力配置\","
                + "\"knowledgeBaseIds\":[" + kbId + "]}";
        MvcResult result = mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.data.id")).longValue();
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
        KnowledgeBaseResponse current = knowledgeBaseService.detail(kbId);
        for (int i = 0; i < 60 && current.status() != expected; i++) {
            Thread.sleep(200);
            current = knowledgeBaseService.detail(kbId);
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
