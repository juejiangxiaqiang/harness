package com.harness.admin.controller;

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
 * 文件说明：知识库接口集成测试。
 *
 * <p>覆盖管理员上传（类型校验）、列表、详情，以及未登录/普通用户的权限边界。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@SpringBootTest
@AutoConfigureMockMvc
class KnowledgeBaseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 验证管理员上传合法文件成功并进入 UPLOADING 状态。
     */
    @Test
    void upload_withAdmin_shouldSucceed() throws Exception {
        String token = loginToken("admin", "Admin@12345");
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "知识库内容".getBytes(StandardCharsets.UTF_8));

        MvcResult result = mockMvc.perform(multipart("/api/knowledge-bases")
                        .file(file)
                        .param("name", "产品手册")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(content, "$.code")).isEqualTo("SUCCESS");
        assertThat(JsonPath.<String>read(content, "$.data.status")).isEqualTo("UPLOADING");
        assertThat(JsonPath.<String>read(content, "$.data.name")).isEqualTo("产品手册");
    }

    /**
     * 验证非法文件类型被拒绝。
     */
    @Test
    void upload_unsupportedType_shouldBeRejected() throws Exception {
        String token = loginToken("admin", "Admin@12345");
        MockMultipartFile file = new MockMultipartFile("file", "virus.exe", "application/octet-stream", "x".getBytes(StandardCharsets.UTF_8));

        MvcResult result = mockMvc.perform(multipart("/api/knowledge-bases")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(content, "$.code")).isEqualTo("VALIDATION_FAILED");
    }

    /**
     * 验证未登录上传被拒绝。
     */
    @Test
    void upload_withoutToken_shouldReturnUnauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "内容".getBytes(StandardCharsets.UTF_8));

        MvcResult result = mockMvc.perform(multipart("/api/knowledge-bases").file(file))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(content, "$.code")).isEqualTo("AUTH_UNAUTHORIZED");
    }

    /**
     * 验证普通用户上传被拒绝（禁止访问）。
     */
    @Test
    void upload_withUserToken_shouldReturnForbidden() throws Exception {
        registerUser("kbuser", "password123");
        String token = loginToken("kbuser", "password123");
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "内容".getBytes(StandardCharsets.UTF_8));

        MvcResult result = mockMvc.perform(multipart("/api/knowledge-bases")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(content, "$.code")).isEqualTo("RESOURCE_FORBIDDEN");
    }

    /**
     * 验证列表与详情查询（管理员），共享资源不按创建人过滤。
     */
    @Test
    void listAndDetail_shouldReturnKnowledgeBases() throws Exception {
        String token = loginToken("admin", "Admin@12345");
        // 预置一条知识库
        MockMultipartFile file = new MockMultipartFile("file", "guide.md", "text/markdown", "指南内容".getBytes(StandardCharsets.UTF_8));
        MvcResult created = mockMvc.perform(multipart("/api/knowledge-bases")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        // 知识库 ID 由 JsonPath 解析为 Number，统一转 long
        long kbId = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.data.id")).longValue();

        MvcResult listResult = mockMvc.perform(get("/api/knowledge-bases").param("page", "1").param("size", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String listContent = listResult.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(listContent, "$.code")).isEqualTo("SUCCESS");
        // 共享测试库中可能存在其他用例写入的数据，仅断言存在记录即可
        assertThat(((Number) JsonPath.read(listContent, "$.data.total")).longValue()).isGreaterThanOrEqualTo(1);

        MvcResult detailResult = mockMvc.perform(get("/api/knowledge-bases/" + kbId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String detailContent = detailResult.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(detailContent, "$.data.fileName")).isEqualTo("guide.md");
    }

    /**
     * 验证查询不存在的知识库返回资源不存在。
     */
    @Test
    void detail_notFound_shouldReturnResourceNotFound() throws Exception {
        String token = loginToken("admin", "Admin@12345");

        MvcResult result = mockMvc.perform(get("/api/knowledge-bases/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(content, "$.code")).isEqualTo("RESOURCE_NOT_FOUND");
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
