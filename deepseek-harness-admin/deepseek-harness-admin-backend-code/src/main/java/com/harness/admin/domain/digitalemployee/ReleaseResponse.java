package com.harness.admin.domain.digitalemployee;

import java.time.LocalDateTime;

/**
 * 文件说明：发布记录响应模型。
 *
 * <p>对外暴露发布版本、状态与失败原因，不暴露完整快照内容。</p>
 *
 * @param id               发布记录 ID
 * @param digitalEmployeeId 数字员工 ID
 * @param version          发布版本号
 * @param status           发布状态
 * @param failureReason    失败原因
 * @param releasedAt       发布时间
 * @author harness-admin
 * @since 2026-10-06
 */
public record ReleaseResponse(
        Long id,
        Long digitalEmployeeId,
        String version,
        ReleaseStatus status,
        String failureReason,
        LocalDateTime releasedAt) {
}
