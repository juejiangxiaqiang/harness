package com.harness.admin.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.harness.admin.domain.user.UserRole;
import com.harness.admin.domain.user.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件说明：用户持久化实体。
 *
 * <p>对应 {@code user} 表。角色与状态为全局字段，不设置个人所有者。
 * 密码仅保存 BCrypt 哈希，禁止明文入库与日志输出。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Data
@TableName("`user`")
public class User {

    /** 用户主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号，唯一 */
    private String username;

    /** 密码哈希（BCrypt） */
    private String passwordHash;

    /** 用户角色 */
    private UserRole role;

    /** 用户状态 */
    private UserStatus status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
