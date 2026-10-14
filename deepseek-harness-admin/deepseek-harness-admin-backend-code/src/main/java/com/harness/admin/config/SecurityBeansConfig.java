package com.harness.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 文件说明：安全组件 Bean 配置。
 *
 * <p>集中提供密码编码器等安全基础设施 Bean。
 * 独立于 {@link WebSecurityConfig}，避免认证服务与拦截器注册之间的循环依赖。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Configuration
public class SecurityBeansConfig {

    /**
     * 提供 BCrypt 密码编码器。
     *
     * @return 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
