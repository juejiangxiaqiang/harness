package com.harness.admin.domain.common;

/**
 * 文件说明：业务异常。
 *
 * <p>Service 层业务规则校验失败时抛出该异常，由全局异常处理器转换为统一失败响应。
 * 携带稳定错误码与面向客户端的中文错误描述，不暴露内部堆栈。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
public class BizException extends RuntimeException {

    /** 业务错误码 */
    private final ErrorCode errorCode;

    /**
     * 构造业务异常，使用错误码默认描述。
     *
     * @param errorCode 业务错误码
     */
    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 构造业务异常，自定义错误描述。
     *
     * @param errorCode 业务错误码
     * @param message   面向客户端的错误描述
     */
    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 获取业务错误码。
     *
     * @return 错误码
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
