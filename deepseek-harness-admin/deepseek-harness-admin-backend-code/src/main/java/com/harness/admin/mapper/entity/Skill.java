package com.harness.admin.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.harness.admin.domain.skill.SkillStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件说明：Skill 持久化实体。
 *
 * <p>对应 {@code skill} 表。Skill 为全局共享资源，created_by 仅用于审计。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Data
@TableName("`skill`")
public class Skill {

    /** Skill 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Skill 名称 */
    private String name;

    /** Skill 描述 */
    private String description;

    /** Skill 配置或实现内容 */
    private String content;

    /** Skill 状态 */
    private SkillStatus status;

    /** Skill 版本 */
    private String version;

    /** 创建管理员 ID，仅用于审计 */
    private Long createdBy;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
