package com.harness.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件说明：认证相关配置。
 *
 * <p>包括凭证有效期、登录防爆破参数与无需认证的白名单路径。
 * 敏感配置（如初始化管理员密码）通过环境变量注入，不写入仓库。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Data
@Component
@ConfigurationProperties(prefix = "harness.auth")
public class AuthProperties {

    /** 凭证有效期（小时） */
    private long tokenExpireHours = 24;

    /** 登录最大失败次数，超过后锁定 */
    private int maxLoginAttempts = 5;

    /** 失败锁定时长（分钟） */
    private int lockMinutes = 15;

    /** 无需认证的白名单路径（精确匹配） */
    private List<String> whitelist = new ArrayList<>(List.of(
            "/api/auth/register", "/api/auth/login", "/api/health"));
}
