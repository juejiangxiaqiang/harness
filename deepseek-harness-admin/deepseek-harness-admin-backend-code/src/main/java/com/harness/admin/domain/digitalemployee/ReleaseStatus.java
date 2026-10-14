package com.harness.admin.domain.digitalemployee;

/**
 * 文件说明：数字员工发布状态枚举。
 *
 * <p>{@code SUCCESS} 表示发布成功并成为可追溯版本；{@code FAILED} 保留失败原因。
 * 发布失败不产生不完整的生效结果，历史快照不可变。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
public enum ReleaseStatus {

    /** 发布成功 */
    SUCCESS,

    /** 发布失败（保留失败原因） */
    FAILED
}
