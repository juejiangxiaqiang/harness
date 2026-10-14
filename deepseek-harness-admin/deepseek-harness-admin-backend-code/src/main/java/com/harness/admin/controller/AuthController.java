package com.harness.admin.controller;

import com.harness.admin.config.BearerTokens;
import com.harness.admin.domain.common.ApiResponse;
import com.harness.admin.domain.user.LoginRequest;
import com.harness.admin.domain.user.LoginResponse;
import com.harness.admin.domain.user.RegisterRequest;
import com.harness.admin.domain.user.UserResponse;
import com.harness.admin.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件说明：认证接口（注册、登录、退出）。
 *
 * <p>注册与登录为公开接口；退出接口要求已认证（由认证拦截器保障）。
 * 角色由服务端决定，请求体不接受角色字段。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** 认证服务 */
    private final AuthService authService;

    /**
     * 构造认证控制器。
     *
     * @param authService 认证服务
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 注册普通用户账号。
     *
     * @param request 注册请求
     * @return 用户基本信息
     */
    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    /**
     * 登录并签发身份凭证。
     *
     * @param request 登录请求
     * @return 凭证与用户基本信息
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    /**
     * 退出登录：吊销当前凭证，重复退出幂等。
     *
     * @param request HTTP 请求
     * @return 成功响应
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        authService.logout(BearerTokens.extract(request));
        return ApiResponse.success();
    }
}
