package com.harness.admin.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.harness.admin.domain.digitalemployee.DigitalEmployeeStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件说明：数字员工持久化实体。
 *
 * <p>对应 {@code digital_employee} 表。数字员工为全局共享资源，
 * 不直接关联知识库，不设置 owner_id；created_by 仅用于审计。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Data
@TableName("`digital_employee`")
public class DigitalEmployee {

    /** 数字员工主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 名称 */
    private String name;

    /** 描述 */
    private String description;

    /** 系统提示词 */
    private String systemPrompt;

    /** 模型配置（JSON 字符串） */
    private String modelConfig;

    /** 状态 */
    private DigitalEmployeeStatus status;

    /** 当前发布版本 */
    private String publishedVersion;

    /** 创建管理员 ID，仅用于审计 */
    private Long createdBy;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
