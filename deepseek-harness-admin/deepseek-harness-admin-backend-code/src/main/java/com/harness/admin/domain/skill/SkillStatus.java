package com.harness.admin.domain.skill;

/**
 * 文件说明：Skill 状态枚举。
 *
 * <p>{@code ACTIVE} 为可用状态，可被数字员工关联；{@code DISABLED} 表示停用。
 * 数字员工关联 Skill 时要求其处于可用状态。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
public enum SkillStatus {

    /** 可用 */
    ACTIVE,

    /** 停用 */
    DISABLED
}
