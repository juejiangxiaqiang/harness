package com.harness.admin.domain.user;

/**
 * 文件说明：登录成功响应模型。
 *
 * <p>返回身份凭证（Bearer Token）与用户基本信息。凭证仅返回一次，
 * 服务端只保存凭证哈希；客户端在受保护接口请求头中以 {@code Authorization: Bearer &lt;token&gt;} 携带。</p>
 *
 * @param token 身份凭证
 * @param user  用户基本信息
 * @author harness-admin
 * @since 2026-10-06
 */
public record LoginResponse(String token, UserResponse user) {
}
