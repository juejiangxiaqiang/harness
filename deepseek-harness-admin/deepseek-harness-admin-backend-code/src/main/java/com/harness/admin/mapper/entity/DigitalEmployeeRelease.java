package com.harness.admin.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.harness.admin.domain.digitalemployee.ReleaseStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件说明：数字员工发布版本持久化实体。
 *
 * <p>对应 {@code digital_employee_release} 表。发布快照不可变，
 * 回滚通过生成新的生效记录切换版本，不得修改历史快照。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Data
@TableName("`digital_employee_release`")
public class DigitalEmployeeRelease {

    /** 发布记录主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 数字员工 ID */
    private Long digitalEmployeeId;

    /** 发布版本号 */
    private String version;

    /** 配置快照（JSON，不可变） */
    private String configSnapshot;

    /** 发布人 ID，仅用于审计 */
    private Long releasedBy;

    /** 发布时间 */
    private LocalDateTime releasedAt;

    /** 发布状态 */
    private ReleaseStatus status;

    /** 失败原因 */
    private String failureReason;
}
