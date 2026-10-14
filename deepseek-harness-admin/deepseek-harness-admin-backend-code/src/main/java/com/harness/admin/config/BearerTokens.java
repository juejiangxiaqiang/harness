package com.harness.admin.config;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 文件说明：Bearer 凭证解析工具。
 *
 * <p>认证拦截器与退出接口共用，保证请求头解析逻辑单一来源：
 * 凭证以 {@code Authorization: Bearer &lt;token&gt;} 形式传递。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
public final class BearerTokens {

    /** Bearer 认证方式前缀 */
    private static final String BEARER_PREFIX = "Bearer ";

    private BearerTokens() {
    }

    /**
     * 从请求头提取原始凭证，缺少或格式不符时返回 null。
     *
     * @param request HTTP 请求
     * @return 原始凭证或 null
     */
    public static String extract(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
