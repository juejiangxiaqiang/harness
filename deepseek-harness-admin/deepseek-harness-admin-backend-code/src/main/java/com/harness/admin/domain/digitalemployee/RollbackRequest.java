package com.harness.admin.domain.digitalemployee;

import jakarta.validation.constraints.NotNull;

/**
 * 文件说明：回滚请求模型。
 *
 * <p>选择历史发布记录回滚；回滚不修改历史快照，而是基于所选快照生成新的生效记录。</p>
 *
 * @param releaseId 历史发布记录 ID
 * @author harness-admin
 * @since 2026-10-06
 */
public record RollbackRequest(
        @NotNull(message = "发布记录 ID 不能为空")
        Long releaseId) {
}
