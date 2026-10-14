package com.harness.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.harness.admin.mapper.entity.Skill;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件说明：Skill 表数据访问接口。
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Mapper
public interface SkillMapper extends BaseMapper<Skill> {
}
