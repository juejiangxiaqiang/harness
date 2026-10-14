package com.harness.admin.domain.user;

/**
 * 文件说明：用户状态枚举。
 *
 * <p>{@code ACTIVE} 为可用状态；{@code DISABLED} 用户不允许登录与访问受保护接口。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
public enum UserStatus {

    /** 可用 */
    ACTIVE,

    /** 禁用 */
    DISABLED
}
