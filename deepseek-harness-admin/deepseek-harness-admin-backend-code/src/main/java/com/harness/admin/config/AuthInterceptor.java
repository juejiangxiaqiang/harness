package com.harness.admin.config;

import com.harness.admin.domain.common.BizException;
import com.harness.admin.domain.common.ErrorCode;
import com.harness.admin.domain.user.AuthContext;
import com.harness.admin.domain.user.UserPrincipal;
import com.harness.admin.domain.user.UserRole;
import com.harness.admin.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 文件说明：认证与角色授权拦截器。
 *
 * <p>作用于全部 {@code /api/**} 路径：白名单路径放行；其余路径要求有效凭证并写入
 * 当前用户上下文（供审计字段使用）；标注 {@link RequireAdmin} 的接口要求 ADMIN 角色。
 * 拦截器内抛出的业务异常由全局异常处理器统一转换为失败响应。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** 认证服务 */
    private final AuthService authService;

    /** 认证配置 */
    private final AuthProperties authProperties;

    /**
     * 构造认证拦截器。
     *
     * @param authService    认证服务
     * @param authProperties 认证配置
     */
    public AuthInterceptor(AuthService authService, AuthProperties authProperties) {
        this.authService = authService;
        this.authProperties = authProperties;
    }

    /**
     * 请求处理前执行认证与角色校验。
     *
     * @param request  请求
     * @param response 响应
     * @param handler  处理器
     * @return 是否继续处理
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 非控制器方法（如静态资源）不参与鉴权
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        String path = request.getRequestURI();
        if (authProperties.getWhitelist().contains(path)) {
            return true;
        }
        UserPrincipal principal = authService.authenticate(BearerTokens.extract(request));
        AuthContext.set(principal);
        MDC.put("userId", String.valueOf(principal.id()));
        MDC.put("role", principal.role().name());
        if (requiresAdmin(handlerMethod) && principal.role() != UserRole.ADMIN) {
            // 普通用户访问管理接口：统一禁止访问错误
            throw new BizException(ErrorCode.RESOURCE_FORBIDDEN, "需要管理员权限");
        }
        return true;
    }

    /**
     * 请求结束后清理用户上下文与日志 MDC，防止线程复用串扰。
     *
     * @param request  请求
     * @param response 响应
     * @param handler  处理器
     * @param ex       异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        AuthContext.clear();
        MDC.remove("userId");
        MDC.remove("role");
    }

    /**
     * 判断处理器是否要求 ADMIN 角色（方法或类级注解）。
     *
     * @param handlerMethod 处理器方法
     * @return 是否要求管理员
     */
    private boolean requiresAdmin(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(RequireAdmin.class)
                || handlerMethod.getBeanType().isAnnotationPresent(RequireAdmin.class);
    }
}
