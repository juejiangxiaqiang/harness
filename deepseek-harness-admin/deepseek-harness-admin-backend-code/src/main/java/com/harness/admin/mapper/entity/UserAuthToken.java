package com.harness.admin.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件说明：用户认证凭证持久化实体。
 *
 * <p>对应 {@code user_auth_token} 表。凭证只保存 SHA-256 哈希，支持过期时间与吊销标记，
 * 退出登录通过吊销凭证实现幂等失效。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Data
@TableName("`user_auth_token`")
public class UserAuthToken {

    /** 凭证主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 ID */
    private Long userId;

    /** 凭证哈希（SHA-256） */
    private String tokenHash;

    /** 过期时间 */
    private LocalDateTime expiresAt;

    /** 是否已吊销 */
    private Boolean revoked;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
