package com.harness.admin.config;

import com.harness.admin.domain.common.ApiResponse;
import com.harness.admin.domain.common.BizException;
import com.harness.admin.domain.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 文件说明：全局统一异常处理器。
 *
 * <p>将业务异常、参数校验异常与未知异常统一转换为 {@link ApiResponse} 失败结构，
 * 保证接口错误格式一致。未知异常记录完整堆栈供排查，但响应不向客户端暴露内部信息。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 结构化日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常：直接透传错误码与错误描述。
     *
     * @param ex 业务异常
     * @return 统一失败响应
     */
    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBizException(BizException ex) {
        return ApiResponse.failure(ex.getErrorCode(), ex.getMessage());
    }

    /**
     * 处理请求体参数校验失败，汇总字段错误信息。
     *
     * @param ex 参数校验异常
     * @return 统一失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse(ErrorCode.VALIDATION_FAILED.getMessage());
        return ApiResponse.failure(ErrorCode.VALIDATION_FAILED, message);
    }

    /**
     * 处理访问不存在的接口路径。
     *
     * @param ex 资源不存在异常
     * @return 统一失败响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ApiResponse<Void> handleNoResourceFoundException(NoResourceFoundException ex) {
        return ApiResponse.failure(ErrorCode.RESOURCE_NOT_FOUND);
    }

    /**
     * 处理未知异常：记录完整堆栈，响应仅返回通用内部错误，避免泄漏内部细节。
     *
     * @param ex 未知异常
     * @return 统一失败响应
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        log.error("unhandled exception", ex);
        return ApiResponse.failure(ErrorCode.INTERNAL_ERROR);
    }
}
