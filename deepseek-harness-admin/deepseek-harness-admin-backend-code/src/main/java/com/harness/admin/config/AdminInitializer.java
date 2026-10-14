package com.harness.admin.config;

import com.harness.admin.domain.common.BizException;
import com.harness.admin.domain.common.ErrorCode;
import com.harness.admin.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 文件说明：管理员账号受控初始化。
 *
 * <p>启动时根据环境变量 {@code harness.bootstrap-admin.username/password} 创建初始管理员。
 * 仅初始化流程可授予 ADMIN 角色，公开注册一律创建 USER；已存在账号时跳过（幂等）。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    /** 结构化日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    /** 认证服务 */
    private final AuthService authService;

    /** 初始化管理员账号（环境变量注入） */
    @Value("${harness.bootstrap-admin.username:}")
    private String adminUsername;

    /** 初始化管理员密码（环境变量注入） */
    @Value("${harness.bootstrap-admin.password:}")
    private String adminPassword;

    /**
     * 构造管理员初始化器。
     *
     * @param authService 认证服务
     */
    public AdminInitializer(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 应用启动后执行管理员账号初始化。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        // 未配置初始化账号则跳过，不自动创建管理员
        if (adminUsername == null || adminUsername.isBlank()) {
            return;
        }
        try {
            authService.registerAdmin(adminUsername, adminPassword);
            log.info("bootstrap admin user created, username={}", adminUsername);
        } catch (BizException ex) {
            // 账号已存在属于幂等预期，不视为启动失败
            if (ex.getErrorCode() == ErrorCode.USERNAME_EXISTS) {
                log.info("bootstrap admin user already exists, username={}", adminUsername);
            } else {
                throw ex;
            }
        }
    }
}
