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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文件说明：Skill 接口集成测试。
 *
 * <p>覆盖管理员创建 Skill（READY 知识库）、非 READY 知识库被拒绝、
 * 未登录/普通用户权限边界、列表与详情。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@SpringBootTest
@AutoConfigureMockMvc
class SkillControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    /**
     * 验证管理员使用 READY 知识库创建 Skill 成功。
     */
    @Test
    void create_withReadyKnowledgeBase_shouldSucceed() throws Exception {
        long kbId = uploadAndAwait("doc.txt", "text/plain", "技能知识库内容", "技能资料", KnowledgeBaseStatus.READY);
        String token = loginToken("admin", "Admin@12345");
        String body = "{\"name\":\"客服助手\",\"description\":\"处理客服咨询\",\"content\":\"能力配置\","
                + "\"knowledgeBaseIds\":[" + kbId + "]}";

        MvcResult result = mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(content, "$.code")).isEqualTo("SUCCESS");
        assertThat(JsonPath.<String>read(content, "$.data.name")).isEqualTo("客服助手");
        assertThat(((Number) JsonPath.read(content, "$.data.knowledgeBaseIds[0]")).longValue()).isEqualTo(kbId);
    }

    /**
     * 验证关联非 READY 知识库时创建被拒绝。
     */
    @Test
    void create_withNotReadyKnowledgeBase_shouldReject() throws Exception {
        // PDF 类型无法解析，最终进入 FAILED，保证非 READY 状态稳定
        long kbId = uploadAndAwait("doc.pdf", "application/pdf", "pdf 内容", "PDF 资料", KnowledgeBaseStatus.FAILED);
        String token = loginToken("admin", "Admin@12345");
        String body = "{\"name\":\"技能\",\"description\":\"\",\"content\":\"内容\","
                + "\"knowledgeBaseIds\":[" + kbId + "]}";

        MvcResult result = mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(content, "$.code")).isEqualTo("KNOWLEDGE_BASE_NOT_READY");
    }

    /**
     * 验证未登录与普通用户访问被拒绝。
     */
    @Test
    void create_withoutAdmin_shouldReject() throws Exception {
        // 未登录
        MvcResult anonymous = mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"技能\",\"knowledgeBaseIds\":[1]}"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(JsonPath.<String>read(anonymous.getResponse().getContentAsString(), "$.code"))
                .isEqualTo("AUTH_UNAUTHORIZED");

        // 普通用户
        registerUser("skilluser", "password123");
        String userToken = loginToken("skilluser", "password123");
        MvcResult forbidden = mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"技能\",\"knowledgeBaseIds\":[1]}")
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
    void listAndDetail_shouldReturnSkills() throws Exception {
        long kbId = uploadAndAwait("doc.txt", "text/plain", "列表测试内容", "列表资料", KnowledgeBaseStatus.READY);
        String token = loginToken("admin", "Admin@12345");
        String body = "{\"name\":\"售后助手\",\"description\":\"售后\",\"content\":\"配置\","
                + "\"knowledgeBaseIds\":[" + kbId + "]}";
        MvcResult created = mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        long skillId = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.data.id")).longValue();

        MvcResult listResult = mockMvc.perform(get("/api/skills").param("page", "1").param("size", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(JsonPath.<String>read(listResult.getResponse().getContentAsString(), "$.code")).isEqualTo("SUCCESS");

        MvcResult detailResult = mockMvc.perform(get("/api/skills/" + skillId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(JsonPath.<String>read(detailResult.getResponse().getContentAsString(), "$.data.name"))
                .isEqualTo("售后助手");
    }

    /**
     * 上传知识库并等待指定终态。
     */
    private long uploadAndAwait(String fileName, String contentType, String content, String name,
                                KnowledgeBaseStatus expected) throws Exception {
        String token = loginToken("admin", "Admin@12345");
        MockMultipartFile file = new MockMultipartFile("file", fileName, contentType,
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
