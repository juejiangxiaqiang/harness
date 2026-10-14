package com.harness.admin.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 文件说明：认证服务单元测试。
 *
 * <p>覆盖注册唯一性与角色边界、登录成功/失败/禁用/锁定、退出幂等、
 * 凭证校验的过期/吊销/禁用路径。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserAuthTokenMapper tokenMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LoginAttemptGuard loginAttemptGuard;

    private AuthProperties authProperties;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authProperties = new AuthProperties();
        authService = new AuthService(userMapper, tokenMapper, passwordEncoder, authProperties, loginAttemptGuard);
    }

    @Test
    void register_shouldCreateUserWithUserRole() {
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        UserResponse response = authService.register(new RegisterRequest("alice", "password123"));

        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.role()).isEqualTo(UserRole.USER);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        // 公开注册角色固定为 USER，不接受请求体指定角色
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.USER);
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void register_duplicateUsername_shouldThrowUsernameExists() {
        User existing = new User();
        existing.setUsername("alice");
        when(userMapper.selectOne(any())).thenReturn(existing);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("alice", "password123")))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USERNAME_EXISTS);
    }

    @Test
    void registerAdmin_shouldCreateAdminRole() {
        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        UserResponse response = authService.registerAdmin("admin", "Admin@12345");

        assertThat(response.role()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void login_success_shouldIssueToken() {
        User user = userWithRole(UserRole.USER, UserStatus.ACTIVE);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(tokenMapper.insert(any(UserAuthToken.class))).thenReturn(1);

        LoginResponse response = authService.login(new LoginRequest("alice", "password123"));

        assertThat(response.token()).isNotBlank();
        assertThat(response.user().role()).isEqualTo(UserRole.USER);
        verify(loginAttemptGuard).reset("alice");
    }

    @Test
    void login_wrongPassword_shouldThrowInvalidCredentials() {
        User user = userWithRole(UserRole.USER, UserStatus.ACTIVE);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong")))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
        verify(loginAttemptGuard).recordFailure("alice");
    }

    @Test
    void login_unknownAccount_shouldThrowInvalidCredentials() {
        when(userMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "password123")))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
    }

    @Test
    void login_disabledAccount_shouldThrowAccountDisabled() {
        User user = userWithRole(UserRole.USER, UserStatus.DISABLED);
        when(userMapper.selectOne(any())).thenReturn(user);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "password123")))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_DISABLED);
    }

    @Test
    void login_whenLocked_shouldThrow() {
        org.mockito.Mockito.doThrow(new BizException(ErrorCode.AUTH_INVALID_CREDENTIALS, "锁定"))
                .when(loginAttemptGuard).checkLocked("alice");

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "password123")))
                .isInstanceOf(BizException.class);
    }

    @Test
    void logout_withNullToken_shouldBeNoOp() {
        // 空凭证退出不抛异常，保证幂等
        authService.logout(null);
        verify(tokenMapper, never()).update(any(), any());
    }

    @Test
    void logout_shouldRevokeToken() {
        authService.logout("raw-token");

        verify(tokenMapper).update(any(), any());
    }

    @Test
    void authenticate_validToken_shouldReturnPrincipal() {
        UserAuthToken token = new UserAuthToken();
        token.setUserId(1L);
        token.setTokenHash("hash");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        token.setRevoked(false);
        when(tokenMapper.selectOne(any())).thenReturn(token);
        User user = userWithRole(UserRole.ADMIN, UserStatus.ACTIVE);
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);

        UserPrincipal principal = authService.authenticate("raw-token");

        assertThat(principal.id()).isEqualTo(1L);
        assertThat(principal.role()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void authenticate_revokedToken_shouldThrowUnauthorized() {
        UserAuthToken token = new UserAuthToken();
        token.setUserId(1L);
        token.setTokenHash("hash");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        token.setRevoked(true);
        when(tokenMapper.selectOne(any())).thenReturn(token);

        assertThatThrownBy(() -> authService.authenticate("raw-token"))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_UNAUTHORIZED);
    }

    @Test
    void authenticate_expiredToken_shouldThrowUnauthorized() {
        UserAuthToken token = new UserAuthToken();
        token.setUserId(1L);
        token.setTokenHash("hash");
        token.setExpiresAt(LocalDateTime.now().minusHours(1));
        token.setRevoked(false);
        when(tokenMapper.selectOne(any())).thenReturn(token);

        assertThatThrownBy(() -> authService.authenticate("raw-token"))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_UNAUTHORIZED);
    }

    @Test
    void authenticate_disabledUser_shouldThrowUnauthorized() {
        UserAuthToken token = new UserAuthToken();
        token.setUserId(1L);
        token.setTokenHash("hash");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        token.setRevoked(false);
        when(tokenMapper.selectOne(any())).thenReturn(token);
        when(userMapper.selectById(1L)).thenReturn(userWithRole(UserRole.USER, UserStatus.DISABLED));

        assertThatThrownBy(() -> authService.authenticate("raw-token"))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_UNAUTHORIZED);
    }

    @Test
    void authenticate_blankToken_shouldThrowUnauthorized() {
        assertThatThrownBy(() -> authService.authenticate(" "))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_UNAUTHORIZED);
    }

    /**
     * 构造指定角色与状态的用户。
     */
    private User userWithRole(UserRole role, UserStatus status) {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setPasswordHash("hashed");
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}
