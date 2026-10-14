package com.harness.admin.domain.digitalemployee;

/**
 * 文件说明：数字员工状态枚举。
 *
 * <p>生命周期：DRAFT（可编辑，未对外）-&gt; PUBLISHED（已发布，可被使用），
 * ARCHIVED 为历史或停用状态。回滚通过切换生效记录实现，不修改历史快照。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
public enum DigitalEmployeeStatus {

    /** 草稿：可编辑，尚未对外提供 */
    DRAFT,

    /** 已发布：完成发布校验，可被使用 */
    PUBLISHED,

    /** 归档：历史或停用状态 */
    ARCHIVED
}
