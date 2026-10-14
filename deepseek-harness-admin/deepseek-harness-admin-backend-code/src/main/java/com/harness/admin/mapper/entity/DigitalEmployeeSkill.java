package com.harness.admin.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 文件说明：数字员工与 Skill 关联持久化实体。
 *
 * <p>对应 {@code digital_employee_skill} 表，联合唯一约束防止重复关联。
 * 数字员工不直接关联知识库，其知识库由关联 Skills 间接提供。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Data
@TableName("`digital_employee_skill`")
public class DigitalEmployeeSkill {

    /** 关联主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 数字员工 ID */
    private Long digitalEmployeeId;

    /** Skill ID */
    private Long skillId;
}
