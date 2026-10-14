package com.harness.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.harness.admin.mapper.entity.PreviewMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件说明：管理端预览对话消息表数据访问接口。
 *
 * @author harness-admin
 * @since 2026-10-08
 */
@Mapper
public interface PreviewMessageMapper extends BaseMapper<PreviewMessage> {
}