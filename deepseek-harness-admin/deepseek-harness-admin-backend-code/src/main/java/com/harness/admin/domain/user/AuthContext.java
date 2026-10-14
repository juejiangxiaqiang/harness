package com.harness.admin.domain.user;

/**
 * 文件说明：当前登录用户上下文。
 *
 * <p>认证拦截器在请求开始时写入当前用户主体，请求结束时清理。
 * 业务层通过该上下文获取操作者（审计字段来源），从而避免信任请求体中的用户标识。</p>
 *
 * <p>ponytail: 当前使用 ThreadLocal 承载请求级上下文，单请求同步处理足够；
 * 若后续引入异步处理（如向量化任务）需要传递身份时，改为显式参数传递或请求属性方案。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
public final class AuthContext {

    /** 当前线程的用户主体 */
    private static final ThreadLocal<UserPrincipal> CURRENT = new ThreadLocal<>();

    private AuthContext() {
    }

    /**
     * 写入当前用户主体。
     *
     * @param principal 用户主体
     */
    public static void set(UserPrincipal principal) {
        CURRENT.set(principal);
    }

    /**
     * 获取当前用户主体，未认证时返回 null。
     *
     * @return 用户主体或 null
     */
    public static UserPrincipal get() {
        return CURRENT.get();
    }

    /**
     * 清理当前用户主体，防止线程复用导致身份串扰。
     */
    public static void clear() {
        CURRENT.remove();
    }
}
