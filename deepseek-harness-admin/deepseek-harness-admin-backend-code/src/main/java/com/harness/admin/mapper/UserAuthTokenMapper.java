package com.harness.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.harness.admin.mapper.entity.UserAuthToken;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件说明：用户认证凭证表数据访问接口。
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Mapper
public interface UserAuthTokenMapper extends BaseMapper<UserAuthToken> {
}
