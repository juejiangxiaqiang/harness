package com.harness.admin.domain.common;

import java.util.List;

/**
 * 文件说明：分页结果模型。
 *
 * <p>列表接口统一返回分页结构，分页参数命名为 page/size，由 Service 层构造。</p>
 *
 * @param <T>   列表元素类型
 * @param page  当前页码（从 1 开始）
 * @param size  每页条数
 * @param total 总条数
 * @param items 当前页数据
 * @author harness-admin
 * @since 2026-10-06
 */
public record PageResult<T>(long page, long size, long total, List<T> items) {
}
