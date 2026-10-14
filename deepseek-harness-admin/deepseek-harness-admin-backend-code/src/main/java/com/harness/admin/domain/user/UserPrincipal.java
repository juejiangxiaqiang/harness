package com.harness.admin.domain.user;

/**
 * 文件说明：当前登录用户主体。
 *
 * <p>由认证拦截器根据有效凭证生成并写入 {@link AuthContext}，业务层只读取该主体，
 * 不信任请求体中的用户或角色字段。创建人、更新人、发布人等审计字段均取自该主体。</p>
 *
 * @param id       用户 ID
 * @param username 登录账号
 * @param role     用户角色
 * @author harness-admin
 * @since 2026-10-06
 */
public record UserPrincipal(Long id, String username, UserRole role) {
}
