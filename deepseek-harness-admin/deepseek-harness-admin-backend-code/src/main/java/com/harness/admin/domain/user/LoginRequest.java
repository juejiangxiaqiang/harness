package com.harness.admin.domain.user;

import jakarta.validation.constraints.NotBlank;

/**
 * 文件说明：登录请求模型。
 *
 * @param username 登录账号
 * @param password 登录密码
 * @author harness-admin
 * @since 2026-10-06
 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空")
        String username,

        @NotBlank(message = "密码不能为空")
        String password) {
}
