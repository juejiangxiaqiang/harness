package com.harness.admin.domain.common;

import org.slf4j.MDC;

/**
 * 文件说明：统一接口响应结构。
 *
 * <p>所有接口（成功或失败）均返回该结构，格式约定见 docs/API_SPEC.md。
 * 响应携带当前请求的 traceId（取自 MDC，由 {@code TraceIdFilter} 写入），便于全链路追踪。</p>
 *
 * @param <T> 业务数据类型
 * @author harness-admin
 * @since 2026-10-06
 */
public class ApiResponse<T> {

    /** MDC 中 traceId 的键名 */
    private static final String TRACE_ID_KEY = "traceId";

    /** 业务错误码 */
    private final String code;

    /** 提示信息 */
    private final String message;

    /** 业务数据，无内容时为 null */
    private final T data;

    /** 请求追踪标识 */
    private final String traceId;

    /**
     * 构造统一响应。
     *
     * @param code    错误码
     * @param message 提示信息
     * @param data    业务数据
     */
    private ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        // 响应 traceId 从日志上下文读取，保证与请求日志关联
        this.traceId = MDC.get(TRACE_ID_KEY);
    }

    /**
     * 构造成功响应（携带数据）。
     *
     * @param data 业务数据
     * @param <T>  数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    /**
     * 构造成功响应（无数据）。
     *
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    /**
     * 构造失败响应。
     *
     * @param errorCode 业务错误码
     * @param message   错误描述
     * @param <T>       数据类型
     * @return 失败响应
     */
    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), message, null);
    }

    /**
     * 构造失败响应，使用错误码默认描述。
     *
     * @param errorCode 业务错误码
     * @param <T>       数据类型
     * @return 失败响应
     */
    public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
        return failure(errorCode, errorCode.getMessage());
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public String getTraceId() {
        return traceId;
    }
}
