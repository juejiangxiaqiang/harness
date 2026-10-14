package com.harness.admin.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件说明：管理端预览对话消息持久化实体。
 *
 * <p>对应 {@code preview_message} 表，保存用户问题与数字员工回复，按会话聚合。</p>
 *
 * @author harness-admin
 * @since 2026-10-08
 */
@Data
@TableName("`preview_message`")
public class PreviewMessage {

    /** 消息主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属会话 ID */
    private Long conversationId;

    /** 消息角色：USER 或 ASSISTANT */
    private String role;

    /** 消息内容 */
    private String content;

    /** 创建时间 */
    private LocalDateTime createdAt;
}