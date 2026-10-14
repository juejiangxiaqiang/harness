package com.harness.admin.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文件说明：健康检查接口集成测试。
 *
 * <p>验证 Spring 容器可加载、接口返回统一成功响应结构，且响应头与响应体携带一致的 traceId。
 * 使用 json-path 断言响应体，避免绑定具体 JSON 序列化实现。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 验证健康检查接口返回统一成功结构与 traceId。
     */
    @Test
    void health_shouldReturnUnifiedSuccessResponse() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        String headerTraceId = result.getResponse().getHeader("X-Trace-Id");

        // 响应体 traceId 与响应头一致，支撑全链路追踪
        assertThat(headerTraceId).isNotBlank();
        assertThat(JsonPath.<String>read(content, "$.code")).isEqualTo("SUCCESS");
        assertThat(JsonPath.<String>read(content, "$.message")).isEqualTo("success");
        assertThat(JsonPath.<String>read(content, "$.traceId")).isEqualTo(headerTraceId);
        assertThat(JsonPath.<String>read(content, "$.data.status")).isEqualTo("UP");
    }

    /**
     * 验证未知接口路径返回统一失败响应结构。
     */
    @Test
    void unknownPath_shouldReturnUnifiedFailureResponse() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/not-exist"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(content, "$.code")).isEqualTo("RESOURCE_NOT_FOUND");
        // 显式转为 Object 避免断言重载歧义
        assertThat((Object) JsonPath.read(content, "$.data")).isNull();
    }
}
