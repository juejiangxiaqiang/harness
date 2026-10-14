package com.harness.admin.service;

import com.harness.admin.config.AuthProperties;
import com.harness.admin.domain.common.BizException;
import com.harness.admin.domain.common.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 文件说明：登录失败次数限制守卫。
 *
 * <p>登录失败连续达到阈值后锁定账号一段时间，防止暴力破解。
 * 成功后重置计数；锁定期间返回统一的凭证错误，不暴露具体账号状态。</p>
 *
 * <p>ponytail: 当前按用户名维度在单机内存计数，单实例部署足够；
 * 若部署多实例需要全局一致计数时，改为基于数据库或 Redis 的计数方案。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Component
public class LoginAttemptGuard {

    /** 单账号的失败计数与锁定时长 */
    private record Attempt(AtomicInteger count, LocalDateTime lockUntil) {
    }

    /** 用户登录失败记录 */
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    /** 认证配置 */
    private final AuthProperties authProperties;

    /**
     * 构造登录防爆破守卫。
     *
     * @param authProperties 认证配置
     */
    public LoginAttemptGuard(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    /**
     * 校验账号是否被锁定，锁定中抛出统一的凭证错误。
     * <p>仅在 count 达到阈值时 lockUntil 才非空；未达阈值时无锁定时间，
     * 直接按未锁定放行，避免对 null 锁定时间取时区比较触发异常。</p>
     *
     * @param username 登录账号
     */
    public void checkLocked(String username) {
        Attempt attempt = attempts.get(username);
        if (attempt != null && attempt.lockUntil() != null && attempt.lockUntil().isAfter(LocalDateTime.now())) {
            throw new BizException(ErrorCode.AUTH_INVALID_CREDENTIALS, "登录失败次数过多，请稍后重试");
        }
    }

    /**
     * 记录一次登录失败；连续失败达到阈值后触发锁定。
     *
     * @param username 登录账号
     */
    public void recordFailure(String username) {
        LocalDateTime now = LocalDateTime.now();
        Attempt attempt = attempts.compute(username, (key, old) -> {
            int count = (old == null || !old.lockUntil().isAfter(now)) ? 1 : old.count().incrementAndGet();
            LocalDateTime lockUntil = null;
            if (count >= authProperties.getMaxLoginAttempts()) {
                lockUntil = now.plusMinutes(authProperties.getLockMinutes());
            }
            return new Attempt(new AtomicInteger(count), lockUntil);
        });
        if (attempt.lockUntil() != null) {
            throw new BizException(ErrorCode.AUTH_INVALID_CREDENTIALS, "登录失败次数过多，请稍后重试");
        }
    }

    /**
     * 登录成功后重置失败计数。
     *
     * @param username 登录账号
     */
    public void reset(String username) {
        attempts.remove(username);
    }
}
