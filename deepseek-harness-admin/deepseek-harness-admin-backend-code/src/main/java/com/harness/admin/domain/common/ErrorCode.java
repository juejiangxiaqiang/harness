package com.harness.admin.domain.common;

/**
 * 文件说明：统一业务错误码定义。
 *
 * <p>所有接口的成功与失败均通过 {@link ApiResponse} 返回，错误码在前后端间保持稳定。
 * 错误码来源：docs/API_SPEC.md 建议错误码表。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
public enum ErrorCode {

    /** 成功 */
    SUCCESS("SUCCESS", "success"),

    /** 登录凭证错误 */
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS", "登录凭证错误"),

    /** 未登录或凭证无效 */
    AUTH_UNAUTHORIZED("AUTH_UNAUTHORIZED", "未登录或凭证无效"),

    /** 账号已存在 */
    USERNAME_EXISTS("USERNAME_EXISTS", "账号已存在"),

    /** 账号已禁用 */
    ACCOUNT_DISABLED("ACCOUNT_DISABLED", "账号已禁用"),

    /** 资源不存在 */
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "资源不存在"),

    /** 无资源访问权限 */
    RESOURCE_FORBIDDEN("RESOURCE_FORBIDDEN", "无资源访问权限"),

    /** 参数校验失败 */
    VALIDATION_FAILED("VALIDATION_FAILED", "参数校验失败"),

    /** 知识库正在向量化处理 */
    KNOWLEDGE_BASE_PROCESSING("KNOWLEDGE_BASE_PROCESSING", "知识库正在向量化处理"),

    /** 知识库尚未准备完成 */
    KNOWLEDGE_BASE_NOT_READY("KNOWLEDGE_BASE_NOT_READY", "知识库尚未准备完成"),

    /** 向量化或向量数据库写入失败 */
    VECTOR_INDEX_FAILED("VECTOR_INDEX_FAILED", "向量化或向量数据库写入失败"),

    /** 数字员工不满足发布条件 */
    DIGITAL_EMPLOYEE_NOT_PUBLISHABLE("DIGITAL_EMPLOYEE_NOT_PUBLISHABLE", "数字员工不满足发布条件"),

    /** 对话模型未配置 */
    CHAT_PROVIDER_NOT_CONFIGURED("CHAT_PROVIDER_NOT_CONFIGURED", "对话模型未配置"),

    /** 对话生成失败 */
    CHAT_FAILED("CHAT_FAILED", "对话生成失败"),

    /** 系统内部错误 */
    INTERNAL_ERROR("INTERNAL_ERROR", "系统内部错误");

    /** 错误码字符串值 */
    private final String code;

    /** 默认错误描述 */
    private final String message;

    /**
     * 构造错误码。
     *
     * @param code    错误码值，前后端约定一致
     * @param message 默认错误描述
     */
    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取错误码值。
     *
     * @return 错误码字符串
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取默认错误描述。
     *
     * @return 错误描述
     */
    public String getMessage() {
        return message;
    }
}
