package com.harness.admin.domain.user;

/**
 * 文件说明：用户基本信息响应模型。
 *
 * <p>对外暴露用户标识、账号与角色，不返回密码哈希等敏感字段。
 * 由 Service 层从用户实体转换构建。</p>
 *
 * @param id       用户 ID
 * @param username 登录账号
 * @param role     用户角色
 * @author harness-admin
 * @since 2026-10-06
 */
public record UserResponse(Long id, String username, UserRole role) {
}
