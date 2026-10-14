package com.harness.admin.domain.common;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 文件说明：统一响应结构单元测试。
 *
 * <p>验证成功/失败响应的错误码、消息、数据与 traceId 行为，覆盖成功与失败两条路径。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
class ApiResponseTest {

    /**
     * 验证成功响应携带数据、成功错误码并透传 MDC traceId。
     */
    @Test
    void success_shouldCarryDataAndCodeAndTraceId() {
        MDC.put("traceId", "trace-001");
        try {
            ApiResponse<String> response = ApiResponse.success("hello");

            assertThat(response.getCode()).isEqualTo("SUCCESS");
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isEqualTo("hello");
            assertThat(response.getTraceId()).isEqualTo("trace-001");
        } finally {
            MDC.remove("traceId");
        }
    }

    /**
     * 验证失败响应使用指定错误码与自定义描述，数据为空。
     */
    @Test
    void failure_shouldCarryErrorCodeAndCustomMessage() {
        ApiResponse<Void> response = ApiResponse.failure(ErrorCode.RESOURCE_NOT_FOUND, "知识库不存在");

        assertThat(response.getCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getMessage()).isEqualTo("知识库不存在");
        assertThat(response.getData()).isNull();
    }

    /**
     * 验证失败响应未设置 traceId 时返回 null，不产生异常。
     */
    @Test
    void failure_withoutTraceId_shouldNotThrow() {
        ApiResponse<Void> response = ApiResponse.failure(ErrorCode.INTERNAL_ERROR);

        assertThat(response.getTraceId()).isNull();
    }
}
