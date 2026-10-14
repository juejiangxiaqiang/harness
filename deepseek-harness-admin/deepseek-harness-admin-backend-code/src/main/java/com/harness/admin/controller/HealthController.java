package com.harness.admin.controller;

import com.harness.admin.domain.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 文件说明：基础健康检查接口。
 *
 * <p>返回服务存活状态与当前时间，供部署探活与工程基线验证使用。
 * 深层依赖（MySQL、向量数据库等）健康由 actuator 健康检查覆盖。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    /**
     * 健康检查：确认服务进程存活。
     *
     * @return 统一成功响应，携带服务状态与当前时间
     */
    @GetMapping
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("status", "UP", "time", OffsetDateTime.now().toString()));
    }
}
