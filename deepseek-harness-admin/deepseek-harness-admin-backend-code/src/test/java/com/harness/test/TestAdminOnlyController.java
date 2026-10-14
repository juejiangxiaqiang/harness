package com.harness.test;

import com.harness.admin.config.RequireAdmin;
import com.harness.admin.domain.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件说明：测试专用管理接口。
 *
 * <p>置于 {@code com.harness.test} 包，避免被主应用组件扫描重复注册；
 * 由集成测试通过 {@code @Bean} 显式注册，用于验证 {@link RequireAdmin} 拦截链路
 * （未登录 401、普通用户 403、管理员放行）。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@RestController
@RequireAdmin
public class TestAdminOnlyController {

    /**
     * 仅管理员可访问的探针接口。
     *
     * @return 成功响应
     */
    @GetMapping("/api/test/admin-only")
    public ApiResponse<String> adminOnly() {
        return ApiResponse.success("ok");
    }
}
