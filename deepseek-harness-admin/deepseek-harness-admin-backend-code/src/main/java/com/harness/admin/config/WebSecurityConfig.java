package com.harness.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 文件说明：Web 安全配置。
 *
 * <p>注册认证拦截器到全部 {@code /api/**} 路径。
 * 密码编码器等安全组件 Bean 见 {@link SecurityBeansConfig}，避免循环依赖。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Configuration
public class WebSecurityConfig implements WebMvcConfigurer {

    /** 认证拦截器 */
    private final AuthInterceptor authInterceptor;

    /**
     * 构造 Web 安全配置。
     *
     * @param authInterceptor 认证拦截器
     */
    public WebSecurityConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    /**
     * 注册认证拦截器到 API 路径。
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
    }
}
