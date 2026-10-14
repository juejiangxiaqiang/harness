package com.harness.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harness.admin.config.AuthProperties;
import com.harness.admin.domain.common.BizException;
import com.harness.admin.domain.common.ErrorCode;
import com.harness.admin.domain.user.LoginRequest;
import com.harness.admin.domain.user.LoginResponse;
import com.harness.admin.domain.user.RegisterRequest;
import com.harness.admin.domain.user.UserPrincipal;
import com.harness.admin.domain.user.UserResponse;
import com.harness.admin.domain.user.UserRole;
import com.harness.admin.domain.user.UserStatus;
import com.harness.admin.mapper.UserAuthTokenMapper;
import com.harness.admin.mapper.UserMapper;
import com.harness.admin.mapper.entity.User;
import com.harness.admin.mapper.entity.UserAuthToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * 文件说明：认证与用户账号服务。
 *
 * <p>职责：注册（服务端决定角色）、登录（签发凭证）、退出（吊销凭证）、
 * 凭证校验（生成当前用户主体）。角色与操作者只取自服务端与认证上下文，不信任请求体。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Service
public class AuthService {

    /** 结构化日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /** 用户表数据访问 */
    private final UserMapper userMapper;

    /** 认证凭证表数据访问 */
    private final UserAuthTokenMapper tokenMapper;

    /** 密码哈希器（BCrypt） */
    private final PasswordEncoder passwordEncoder;

    /** 认证配置 */
    private final AuthProperties authProperties;

    /** 登录防爆破守卫 */
    private final LoginAttemptGuard loginAttemptGuard;

    /**
     * 构造认证服务。
     *
     * @param userMapper        用户表数据访问
     * @param tokenMapper       认证凭证表数据访问
     * @param passwordEncoder   密码哈希器
     * @param authProperties    认证配置
     * @param loginAttemptGuard 登录防爆破守卫
     */
    public AuthService(UserMapper userMapper, UserAuthTokenMapper tokenMapper, PasswordEncoder passwordEncoder,
                       AuthProperties authProperties, LoginAttemptGuard loginAttemptGuard) {
        this.userMapper = userMapper;
        this.tokenMapper = tokenMapper;
        this.passwordEncoder = passwordEncoder;
        this.authProperties = authProperties;
        this.loginAttemptGuard = loginAttemptGuard;
    }

    /**
     * 公开注册：服务端固定创建 USER 角色，请求体不得指定角色。
     *
     * @param request 注册请求
     * @return 用户基本信息
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        // 注册与唯一性校验处于同一事务，先查后插，数据库唯一索引兜底并发场景
        if (findByUsername(request.username()) != null) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            // 并发注册同账号时命中唯一索引，仍按业务错误处理
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        log.info("user registered, userId={}, username={}, role={}",
                user.getId(), user.getUsername(), user.getRole());
        return new UserResponse(user.getId(), user.getUsername(), user.getRole());
    }

    /**
     * 受控初始化管理员账号：仅初始化流程调用，不得通过公开注册授予。
     *
     * @param username 管理员账号
     * @param password 管理员密码
     * @return 用户基本信息
     */
    @Transactional
    public UserResponse registerAdmin(String username, String password) {
        if (findByUsername(username) != null) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(UserRole.ADMIN);
        user.setStatus(UserStatus.ACTIVE);
        userMapper.insert(user);
        log.info("admin user initialized, userId={}, username={}", user.getId(), user.getUsername());
        return new UserResponse(user.getId(), user.getUsername(), user.getRole());
    }

    /**
     * 登录：校验账号状态与密码，成功签发身份凭证。
     *
     * @param request 登录请求
     * @return 凭证与用户基本信息
     */
    public LoginResponse login(LoginRequest request) {
        loginAttemptGuard.checkLocked(request.username());
        User user = findByUsername(request.username());
        if (user == null) {
            loginAttemptGuard.recordFailure(request.username());
            // 账号不存在与密码错误使用同错误码、不同描述，便于客户端区分又不扩大代码面
            throw new BizException(ErrorCode.AUTH_INVALID_CREDENTIALS, "账号不存在");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptGuard.recordFailure(request.username());
            throw new BizException(ErrorCode.AUTH_INVALID_CREDENTIALS, "密码错误");
        }
        loginAttemptGuard.reset(request.username());
        String rawToken = issueToken(user.getId());
        log.info("user logged in, userId={}, username={}, role={}",
                user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(rawToken, new UserResponse(user.getId(), user.getUsername(), user.getRole()));
    }

    /**
     * 退出登录：吊销当前凭证，重复退出不产生异常（幂等）。
     *
     * @param rawToken 原始凭证
     */
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        // 幂等吊销：无论凭证是否存在、是否已吊销，均视为退出成功
        UserAuthToken update = new UserAuthToken();
        update.setRevoked(true);
        tokenMapper.update(update, new LambdaQueryWrapper<UserAuthToken>()
                .eq(UserAuthToken::getTokenHash, sha256(rawToken)));
        log.info("user logged out");
    }

    /**
     * 校验凭证并返回当前用户主体；凭证无效、过期、已吊销或用户不可用时抛出未授权。
     *
     * @param rawToken 原始凭证
     * @return 当前用户主体
     */
    public UserPrincipal authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BizException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        UserAuthToken token = tokenMapper.selectOne(new LambdaQueryWrapper<UserAuthToken>()
                .eq(UserAuthToken::getTokenHash, sha256(rawToken)));
        if (token == null || Boolean.TRUE.equals(token.getRevoked())
                || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        User user = userMapper.selectById(token.getUserId());
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            throw new BizException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        return new UserPrincipal(user.getId(), user.getUsername(), user.getRole());
    }

    /**
     * 按账号查询用户。
     *
     * @param username 登录账号
     * @return 用户或 null
     */
    public User findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    /**
     * 生成并持久化凭证：数据库只保存 SHA-256 哈希，原始凭证仅返回一次。
     *
     * @param userId 用户 ID
     * @return 原始凭证
     */
    private String issueToken(Long userId) {
        // 顺带清理该用户的过期凭证，避免表无限增长
        tokenMapper.delete(new LambdaQueryWrapper<UserAuthToken>()
                .eq(UserAuthToken::getUserId, userId)
                .lt(UserAuthToken::getExpiresAt, LocalDateTime.now()));
        String rawToken = UUID.randomUUID().toString().replace("-", "");
        UserAuthToken token = new UserAuthToken();
        token.setUserId(userId);
        token.setTokenHash(sha256(rawToken));
        token.setExpiresAt(LocalDateTime.now().plusHours(authProperties.getTokenExpireHours()));
        token.setRevoked(false);
        tokenMapper.insert(token);
        return rawToken;
    }

    /**
     * 计算 SHA-256 十六进制摘要。
     *
     * @param value 原始值
     * @return 哈希十六进制串
     */
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            // JDK 17 必然支持 SHA-256，此处仅防御性兜底
            throw new IllegalStateException("SHA-256 算法不可用", ex);
        }
    }
}
