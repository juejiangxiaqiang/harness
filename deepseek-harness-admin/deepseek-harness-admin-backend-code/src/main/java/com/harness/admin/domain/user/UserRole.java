package com.harness.admin.domain.user;

/**
 * 文件说明：用户角色枚举。
 *
 * <p>{@code ADMIN} 可访问管理后台全部管理接口；{@code USER} 仅能使用已发布数字员工（独立用户端）。
 * 角色由服务端受控初始化或授权流程授予，公开注册不得指定角色。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
public enum UserRole {

    /** 管理员：上传知识库、维护 Skills、数字员工与发布回滚 */
    ADMIN,

    /** 普通用户：只能使用已发布数字员工 */
    USER
}
