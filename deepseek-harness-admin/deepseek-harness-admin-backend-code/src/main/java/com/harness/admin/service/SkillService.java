package com.harness.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.harness.admin.domain.common.BizException;
import com.harness.admin.domain.common.ErrorCode;
import com.harness.admin.domain.common.PageResult;
import com.harness.admin.domain.knowledgebase.KnowledgeBaseStatus;
import com.harness.admin.domain.skill.SkillRequest;
import com.harness.admin.domain.skill.SkillResponse;
import com.harness.admin.domain.skill.SkillStatus;
import com.harness.admin.domain.user.AuthContext;
import com.harness.admin.domain.user.UserPrincipal;
import com.harness.admin.mapper.KnowledgeBaseMapper;
import com.harness.admin.mapper.SkillKnowledgeBaseMapper;
import com.harness.admin.mapper.SkillMapper;
import com.harness.admin.mapper.entity.KnowledgeBase;
import com.harness.admin.mapper.entity.Skill;
import com.harness.admin.mapper.entity.SkillKnowledgeBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件说明：Skill 服务。
 *
 * <p>职责：创建 Skill（校验关联知识库存在且 READY，主体与关联同一事务写入）、
 * 分页列表与详情（附带关联知识库 ID）。Skill 为全局共享资源，不按创建人过滤。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Service
public class SkillService {

    /** 结构化日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(SkillService.class);

    /** 单页大小上限 */
    private static final long MAX_PAGE_SIZE = 100;

    /** 默认版本号 */
    private static final String DEFAULT_VERSION = "1.0";

    /** Skill 表数据访问 */
    private final SkillMapper skillMapper;

    /** Skill-知识库关联表数据访问 */
    private final SkillKnowledgeBaseMapper associationMapper;

    /** 知识库表数据访问 */
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    /**
     * 构造 Skill 服务。
     *
     * @param skillMapper          Skill 表数据访问
     * @param associationMapper    关联表数据访问
     * @param knowledgeBaseMapper  知识库表数据访问
     */
    public SkillService(SkillMapper skillMapper, SkillKnowledgeBaseMapper associationMapper,
                        KnowledgeBaseMapper knowledgeBaseMapper) {
        this.skillMapper = skillMapper;
        this.associationMapper = associationMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
    }

    /**
     * 创建 Skill：校验知识库存在且 READY，Skill 与关联关系同一事务写入。
     *
     * @param request 创建请求
     * @return Skill 信息
     */
    @Transactional
    public SkillResponse create(SkillRequest request) {
        UserPrincipal operator = AuthContext.get();
        if (operator == null) {
            // 拦截器应已保证认证，此处仅防御性校验
            throw new BizException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        // 去重后的知识库 ID，避免重复关联触发唯一约束
        List<Long> kbIds = request.knowledgeBaseIds().stream().distinct().toList();
        validateKnowledgeBases(kbIds);

        Skill skill = new Skill();
        skill.setName(request.name().trim());
        skill.setDescription(request.description() == null ? "" : request.description().trim());
        skill.setContent(request.content().trim());
        skill.setStatus(SkillStatus.ACTIVE);
        skill.setVersion(DEFAULT_VERSION);
        skill.setCreatedBy(operator.id());
        skillMapper.insert(skill);
        // 主体与关联关系同事务，避免出现 Skill 无知识库的中间态
        for (Long kbId : kbIds) {
            SkillKnowledgeBase association = new SkillKnowledgeBase();
            association.setSkillId(skill.getId());
            association.setKnowledgeBaseId(kbId);
            associationMapper.insert(association);
        }
        log.info("skill created, skillId={}, name={}, kbCount={}, createdBy={}",
                skill.getId(), skill.getName(), kbIds.size(), skill.getCreatedBy());
        return toResponse(skill, kbIds);
    }

    /**
     * 分页查询共享 Skill 列表（附带各 Skill 关联知识库 ID）。
     *
     * @param page 页码
     * @param size 每页条数
     * @return 分页结果
     */
    public PageResult<SkillResponse> list(long page, long size) {
        long safePage = Math.max(page, 1);
        long safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<Skill> result = skillMapper.selectPage(new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<Skill>().orderByDesc(Skill::getCreatedAt));
        // 批量加载本页 Skill 的关联，避免逐条 N+1 查询
        List<Long> skillIds = result.getRecords().stream().map(Skill::getId).toList();
        Map<Long, List<Long>> associationsBySkill = loadAssociationsBySkill(skillIds);
        List<SkillResponse> items = result.getRecords().stream()
                .map(skill -> toResponse(skill, associationsBySkill.getOrDefault(skill.getId(), List.of())))
                .toList();
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), items);
    }

    /**
     * 查询 Skill 详情（含关联知识库 ID）。
     *
     * @param id Skill ID
     * @return Skill 信息
     */
    public SkillResponse detail(Long id) {
        Skill skill = skillMapper.selectById(id);
        if (skill == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "Skill 不存在");
        }
        List<Long> kbIds = associationMapper.selectList(new LambdaQueryWrapper<SkillKnowledgeBase>()
                        .eq(SkillKnowledgeBase::getSkillId, id))
                .stream().map(SkillKnowledgeBase::getKnowledgeBaseId).toList();
        return toResponse(skill, kbIds);
    }

    /**
     * 校验知识库均存在且处于 READY 状态。
     *
     * @param kbIds 知识库 ID 列表
     */
    private void validateKnowledgeBases(List<Long> kbIds) {
        for (Long kbId : kbIds) {
            KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
            if (kb == null) {
                throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "知识库不存在: " + kbId);
            }
            // 仅 READY 知识库可被 Skill 关联
            if (kb.getStatus() != KnowledgeBaseStatus.READY) {
                throw new BizException(ErrorCode.KNOWLEDGE_BASE_NOT_READY, "知识库尚未准备完成: " + kbId);
            }
        }
    }

    /**
     * 批量加载多个 Skill 的关联知识库 ID。
     *
     * @param skillIds Skill ID 列表
     * @return Skill ID 到知识库 ID 列表的映射
     */
    private Map<Long, List<Long>> loadAssociationsBySkill(List<Long> skillIds) {
        Map<Long, List<Long>> result = new LinkedHashMap<>();
        if (skillIds.isEmpty()) {
            return result;
        }
        List<SkillKnowledgeBase> associations = associationMapper.selectList(
                new LambdaQueryWrapper<SkillKnowledgeBase>().in(SkillKnowledgeBase::getSkillId, skillIds));
        for (SkillKnowledgeBase association : associations) {
            result.computeIfAbsent(association.getSkillId(), key -> new ArrayList<>())
                    .add(association.getKnowledgeBaseId());
        }
        return result;
    }

    /**
     * 实体转响应模型。
     *
     * @param skill Skill 实体
     * @param kbIds 关联知识库 ID
     * @return Skill 响应
     */
    private SkillResponse toResponse(Skill skill, List<Long> kbIds) {
        return new SkillResponse(skill.getId(), skill.getName(), skill.getDescription(), skill.getContent(),
                skill.getStatus(), skill.getVersion(), kbIds, skill.getCreatedAt());
    }
}
