package com.harness.admin.domain.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 文件说明：注册请求模型。
 *
 * <p>请求体只允许账号与密码，不得包含角色字段；角色由服务端策略决定（公开注册固定为 USER）。
 * 防止客户端通过注册流程自助提升为管理员。</p>
 *
 * @param username 登录账号，字母数字，长度 3-32
 * @param password 登录密码，长度 8-64
 * @author harness-admin
 * @since 2026-10-06
 */
public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "^[a-zA-Z0-9_]{3,32}$", message = "用户名仅支持字母、数字、下划线，长度 3-32")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度需在 8-64 之间")
        String password) {
}
