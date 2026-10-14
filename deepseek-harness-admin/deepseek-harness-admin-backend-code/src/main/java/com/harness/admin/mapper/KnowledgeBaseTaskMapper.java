package com.harness.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.harness.admin.mapper.entity.KnowledgeBaseTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件说明：知识库向量化任务表数据访问接口。
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Mapper
public interface KnowledgeBaseTaskMapper extends BaseMapper<KnowledgeBaseTask> {
}
