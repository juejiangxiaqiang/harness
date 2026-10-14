package com.harness.admin.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件说明：管理端预览会话持久化实体。
 *
 * <p>对应 {@code preview_conversation} 表，用于持久化管理员与数字员工的对话会话，
 * 支撑对话历史查询。会话按数字员工维度聚合，created_by 仅用于审计。</p>
 *
 * @author harness-admin
 * @since 2026-10-08
 */
@Data
@TableName("`preview_conversation`")
public class PreviewConversation {

    /** 会话主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 数字员工 ID（对话来源） */
    private Long digitalEmployeeId;

    /** 会话标题（取首条问题摘要） */
    private String title;

    /** 发起会话的管理员 ID，仅用于审计 */
    private Long createdBy;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 最近消息时间 */
    private LocalDateTime updatedAt;
}