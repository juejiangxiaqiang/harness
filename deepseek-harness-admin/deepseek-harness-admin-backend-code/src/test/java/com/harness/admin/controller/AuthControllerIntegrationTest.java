package com.harness.admin.controller;

import com.harness.admin.domain.common.ApiResponse;
import com.harness.test.TestAdminOnlyController;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文件说明：认证接口集成测试。
 *
 * <p>覆盖注册、登录、退出与权限边界：未认证返回未授权、普通用户访问管理接口
 * 返回禁止访问、管理员可访问管理接口。使用测试专用管理接口验证拦截器链路。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthControllerIntegrationTest.TestAdminConfig.class)
class AuthControllerIntegrationTest {

    /** 测试专用配置：注册一个仅管理员可访问的测试接口 */
    @TestConfiguration
    static class TestAdminConfig {

        /**
         * 注册测试用管理接口 Bean（该接口位于测试独立包，避免组件扫描重复注册）。
         *
         * @return 测试管理接口
         */
        @Bean
        TestAdminOnlyController testAdminOnlyController() {
            return new TestAdminOnlyController();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    /**
     * 验证注册成功且角色由服务端固定为 USER。
     */
    @Test
    void register_shouldSucceedWithUserRole() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(content, "$.code")).isEqualTo("SUCCESS");
        assertThat(JsonPath.<String>read(content, "$.data.username")).isEqualTo("alice");
        assertThat(JsonPath.<String>read(content, "$.data.role")).isEqualTo("USER");
    }

    /**
     * 验证重复注册返回账号已存在错误。
     */
    @Test
    void register_duplicate_shouldReturnUsernameExists() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"bob\",\"password\":\"password123\"}"));

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bob\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(content, "$.code")).isEqualTo("USERNAME_EXISTS");
    }

    /**
     * 验证登录成功返回凭证。
     */
    @Test
    void login_shouldReturnToken() throws Exception {
        registerUser("carol", "password123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"carol\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(content, "$.data.token")).isNotBlank();
        assertThat(JsonPath.<String>read(content, "$.data.user.role")).isEqualTo("USER");
    }

    /**
     * 验证密码错误返回凭证错误。
     */
    @Test
    void login_wrongPassword_shouldReturnInvalidCredentials() throws Exception {
        registerUser("dave", "password123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dave\",\"password\":\"wrong-pass\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(content, "$.code")).isEqualTo("AUTH_INVALID_CREDENTIALS");
    }

    /**
     * 验证未登录访问管理接口返回未授权。
     */
    @Test
    void adminOnly_withoutToken_shouldReturnUnauthorized() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/test/admin-only"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(content, "$.code")).isEqualTo("AUTH_UNAUTHORIZED");
    }

    /**
     * 验证普通用户访问管理接口返回禁止访问。
     */
    @Test
    void adminOnly_withUserToken_shouldReturnForbidden() throws Exception {
        registerUser("erin", "password123");
        String token = loginToken("erin", "password123");

        MvcResult result = mockMvc.perform(get("/api/test/admin-only")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(content, "$.code")).isEqualTo("RESOURCE_FORBIDDEN");
    }

    /**
     * 验证管理员（受控初始化账号）可访问管理接口。
     */
    @Test
    void adminOnly_withAdminToken_shouldSucceed() throws Exception {
        String token = loginToken("admin", "Admin@12345");

        MvcResult result = mockMvc.perform(get("/api/test/admin-only")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(content, "$.code")).isEqualTo("SUCCESS");
    }

    /**
     * 验证退出登录后凭证失效，访问受保护接口被拒绝。
     */
    @Test
    void logout_shouldInvalidateToken() throws Exception {
        registerUser("frank", "password123");
        String token = loginToken("frank", "password123");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/test/admin-only")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(content, "$.code")).isEqualTo("AUTH_UNAUTHORIZED");
    }

    /**
     * 验证未登录调用退出接口被拒绝（退出接口要求认证）。
     */
    @Test
    void logout_withoutToken_shouldReturnUnauthorized() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(content, "$.code")).isEqualTo("AUTH_UNAUTHORIZED");
    }

    /**
     * 注册测试用户。
     *
     * @param username 账号
     * @param password 密码
     */
    private void registerUser(String username, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk());
    }

    /**
     * 登录并提取凭证。
     *
     * @param username 账号
     * @param password 密码
     * @return 原始凭证
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
