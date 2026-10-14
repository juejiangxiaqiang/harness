package com.harness.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.harness.admin.domain.common.BizException;
import com.harness.admin.domain.common.ErrorCode;
import com.harness.admin.domain.common.PageResult;
import com.harness.admin.domain.digitalemployee.DigitalEmployeeRequest;
import com.harness.admin.domain.digitalemployee.DigitalEmployeeResponse;
import com.harness.admin.domain.digitalemployee.DigitalEmployeeStatus;
import com.harness.admin.domain.skill.SkillStatus;
import com.harness.admin.domain.user.AuthContext;
import com.harness.admin.domain.user.UserPrincipal;
import com.harness.admin.domain.user.UserRole;
import com.harness.admin.mapper.DigitalEmployeeMapper;
import com.harness.admin.mapper.DigitalEmployeeSkillMapper;
import com.harness.admin.mapper.SkillMapper;
import com.harness.admin.mapper.entity.DigitalEmployee;
import com.harness.admin.mapper.entity.DigitalEmployeeSkill;
import com.harness.admin.mapper.entity.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件说明：数字员工服务。
 *
 * <p>职责：创建数字员工（校验关联 Skill 存在且可用，主体与关联同事务写入）、
 * 分页列表与详情（附带 Skill ID）。数字员工不直接关联知识库，
 * 请求体不接收知识库 ID。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Service
public class DigitalEmployeeService {

    /** 结构化日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(DigitalEmployeeService.class);

    /** 单页大小上限 */
    private static final long MAX_PAGE_SIZE = 100;

    /** 数字员工表数据访问 */
    private final DigitalEmployeeMapper employeeMapper;

    /** 数字员工-Skill 关联表数据访问 */
    private final DigitalEmployeeSkillMapper associationMapper;

    /** Skill 表数据访问 */
    private final SkillMapper skillMapper;

    /** JSON 序列化器（模型配置存储） */
    private final ObjectMapper objectMapper;

    /**
     * 构造数字员工服务。
     *
     * @param employeeMapper   数字员工表数据访问
     * @param associationMapper 关联表数据访问
     * @param skillMapper       Skill 表数据访问
     * @param objectMapper      JSON 序列化器
     */
    public DigitalEmployeeService(DigitalEmployeeMapper employeeMapper, DigitalEmployeeSkillMapper associationMapper,
                                  SkillMapper skillMapper, ObjectMapper objectMapper) {
        this.employeeMapper = employeeMapper;
        this.associationMapper = associationMapper;
        this.skillMapper = skillMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建数字员工：校验 Skill 存在且可用，主体与关联同事务写入。
     *
     * @param request 创建请求
     * @return 数字员工信息
     */
    @Transactional
    public DigitalEmployeeResponse create(DigitalEmployeeRequest request) {
        UserPrincipal operator = AuthContext.get();
        if (operator == null) {
            // 拦截器应已保证认证，此处仅防御性校验
            throw new BizException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        // 去重后的 Skill ID，避免重复关联
        List<Long> skillIds = request.skillIds().stream().distinct().toList();
        validateSkills(skillIds);

        DigitalEmployee employee = new DigitalEmployee();
        employee.setName(request.name().trim());
        employee.setDescription(request.description() == null ? "" : request.description().trim());
        employee.setSystemPrompt(request.systemPrompt().trim());
        employee.setModelConfig(toJson(request.modelConfig()));
        employee.setStatus(DigitalEmployeeStatus.DRAFT);
        employee.setCreatedBy(operator.id());
        employeeMapper.insert(employee);
        for (Long skillId : skillIds) {
            DigitalEmployeeSkill association = new DigitalEmployeeSkill();
            association.setDigitalEmployeeId(employee.getId());
            association.setSkillId(skillId);
            associationMapper.insert(association);
        }
        log.info("digital employee created, employeeId={}, name={}, skillCount={}, createdBy={}",
                employee.getId(), employee.getName(), skillIds.size(), employee.getCreatedBy());
        return toResponse(employee, skillIds);
    }

    /**
     * 分页查询数字员工列表（附带各数字员工关联 Skill ID）。
     *
     * <p>普通用户仅可见已发布（PUBLISHED）数字员工；管理员可见全部。</p>
     *
     * @param page 页码
     * @param size 每页条数
     * @return 分页结果
     */
    public PageResult<DigitalEmployeeResponse> list(long page, long size) {
        long safePage = Math.max(page, 1);
        long safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        LambdaQueryWrapper<DigitalEmployee> query = new LambdaQueryWrapper<DigitalEmployee>()
                .orderByDesc(DigitalEmployee::getCreatedAt);
        if (isUser()) {
            query.eq(DigitalEmployee::getStatus, DigitalEmployeeStatus.PUBLISHED);
        }
        Page<DigitalEmployee> result = employeeMapper.selectPage(new Page<>(safePage, safeSize), query);
        List<Long> employeeIds = result.getRecords().stream().map(DigitalEmployee::getId).toList();
        Map<Long, List<Long>> associationsByEmployee = loadAssociationsByEmployee(employeeIds);
        List<DigitalEmployeeResponse> items = result.getRecords().stream()
                .map(employee -> toResponse(employee,
                        associationsByEmployee.getOrDefault(employee.getId(), List.of())))
                .toList();
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), items);
    }

    /**
     * 查询数字员工详情（含关联 Skill ID）。
     *
     * <p>普通用户仅可查看已发布数字员工，未发布或不存在均返回资源不存在（不泄露存在性）。</p>
     *
     * @param id 数字员工 ID
     * @return 数字员工信息
     */
    public DigitalEmployeeResponse detail(Long id) {
        DigitalEmployee employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "数字员工不存在");
        }
        if (isUser() && employee.getStatus() != DigitalEmployeeStatus.PUBLISHED) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "数字员工不存在");
        }
        List<Long> skillIds = associationMapper.selectList(new LambdaQueryWrapper<DigitalEmployeeSkill>()
                        .eq(DigitalEmployeeSkill::getDigitalEmployeeId, id))
                .stream().map(DigitalEmployeeSkill::getSkillId).toList();
        return toResponse(employee, skillIds);
    }

    /**
     * 校验 Skill 均存在且处于可用状态。
     *
     * @param skillIds Skill ID 列表
     */
    private void validateSkills(List<Long> skillIds) {
        for (Long skillId : skillIds) {
            Skill skill = skillMapper.selectById(skillId);
            if (skill == null) {
                throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "Skill 不存在: " + skillId);
            }
            // 仅可用 Skill 可被数字员工关联
            if (skill.getStatus() != SkillStatus.ACTIVE) {
                throw new BizException(ErrorCode.VALIDATION_FAILED, "Skill 不可用: " + skillId);
            }
        }
    }

    /**
     * 批量加载多个数字员工的关联 Skill ID。
     *
     * @param employeeIds 数字员工 ID 列表
     * @return 数字员工 ID 到 Skill ID 列表的映射
     */
    private Map<Long, List<Long>> loadAssociationsByEmployee(List<Long> employeeIds) {
        Map<Long, List<Long>> result = new LinkedHashMap<>();
        if (employeeIds.isEmpty()) {
            return result;
        }
        List<DigitalEmployeeSkill> associations = associationMapper.selectList(
                new LambdaQueryWrapper<DigitalEmployeeSkill>()
                        .in(DigitalEmployeeSkill::getDigitalEmployeeId, employeeIds));
        for (DigitalEmployeeSkill association : associations) {
            result.computeIfAbsent(association.getDigitalEmployeeId(), key -> new ArrayList<>())
                    .add(association.getSkillId());
        }
        return result;
    }

    /**
     * 模型配置序列化为 JSON 字符串。
     *
     * @param modelConfig 模型配置
     * @return JSON 字符串
     */
    private String toJson(Map<String, Object> modelConfig) {
        try {
            return objectMapper.writeValueAsString(modelConfig);
        } catch (Exception ex) {
            throw new IllegalStateException("模型配置序列化失败", ex);
        }
    }

    /**
     * JSON 字符串反序列化为模型配置。
     *
     * @param json JSON 字符串
     * @return 模型配置
     */
    private Map<String, Object> fromJson(String json) {
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructMapType(
                    LinkedHashMap.class, String.class, Object.class));
        } catch (Exception ex) {
            log.warn("parse model config failed, employeeId={}", json, ex);
            return Map.of();
        }
    }

    /**
     * 实体转响应模型。
     *
     * @param employee 数字员工实体
     * @param skillIds 关联 Skill ID
     * @return 数字员工响应
     */
    private DigitalEmployeeResponse toResponse(DigitalEmployee employee, List<Long> skillIds) {
        return new DigitalEmployeeResponse(employee.getId(), employee.getName(), employee.getDescription(),
                employee.getSystemPrompt(), fromJson(employee.getModelConfig()), employee.getStatus(),
                employee.getPublishedVersion(), skillIds, employee.getCreatedAt());
    }

    /**
     * 判断当前操作者是否为普通用户（USER 角色）。
     *
     * @return 普通用户返回 true
     */
    private boolean isUser() {
        UserPrincipal operator = AuthContext.get();
        return operator != null && operator.role() == UserRole.USER;
    }
}
