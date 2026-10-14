package com.harness.admin.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 文件说明：请求 traceId 过滤器。
 *
 * <p>为每个请求生成或透传 traceId（请求头 X-Trace-Id），写入日志上下文 MDC，
 * 供结构化日志与统一响应引用；响应头回写同一 traceId，便于全链路追踪。
 * 请求结束后清理 MDC，避免线程复用导致 traceId 串扰。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    /** traceId 请求头与 MDC 键名 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /** MDC 中的 traceId 键名 */
    public static final String TRACE_ID_KEY = "traceId";

    /**
     * 处理单个请求：生成 traceId、写入 MDC 与响应头，完成后清理 MDC。
     *
     * @param request     请求
     * @param response    响应
     * @param filterChain 过滤器链
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        MDC.put(TRACE_ID_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 清理 MDC，防止线程池复用线程时残留上一个请求的 traceId
            MDC.remove(TRACE_ID_KEY);
        }
    }

    /**
     * 解析 traceId：优先透传客户端携带值，否则生成新值。
     *
     * @param request 请求
     * @return traceId
     */
    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        return (traceId == null || traceId.isBlank()) ? UUID.randomUUID().toString() : traceId;
    }
}
