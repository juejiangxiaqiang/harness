package com.harness.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harness.admin.domain.common.BizException;
import com.harness.admin.domain.common.ErrorCode;
import com.harness.admin.domain.knowledgebase.KnowledgeBaseStatus;
import com.harness.admin.domain.skill.SkillStatus;
import com.harness.admin.mapper.DigitalEmployeeSkillMapper;
import com.harness.admin.mapper.KnowledgeBaseMapper;
import com.harness.admin.mapper.SkillKnowledgeBaseMapper;
import com.harness.admin.mapper.SkillMapper;
import com.harness.admin.mapper.entity.DigitalEmployeeSkill;
import com.harness.admin.mapper.entity.KnowledgeBase;
import com.harness.admin.mapper.entity.Skill;
import com.harness.admin.mapper.entity.SkillKnowledgeBase;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件说明：发布配置快照构建与校验服务。
 *
 * <p>负责发布前展开并校验 Skills 与知识库、构建不可变快照 JSON、
 * 以及回滚时校验历史快照引用的资源可用性。从发布服务拆分，控制单文件规模。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Component
public class ReleaseSnapshotService {

    /** 数字员工-Skill 关联表数据访问 */
    private final DigitalEmployeeSkillMapper employeeSkillMapper;

    /** Skill 表数据访问 */
    private final SkillMapper skillMapper;

    /** Skill-知识库关联表数据访问 */
    private final SkillKnowledgeBaseMapper skillKbMapper;

    /** 知识库表数据访问 */
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    /** JSON 序列化器 */
    private final ObjectMapper objectMapper;

    /**
     * 构造快照服务。
     *
     * @param employeeSkillMapper 数字员工-Skill 关联表数据访问
     * @param skillMapper         Skill 表数据访问
     * @param skillKbMapper       Skill-知识库关联表数据访问
     * @param knowledgeBaseMapper 知识库表数据访问
     * @param objectMapper        JSON 序列化器
     */
    public ReleaseSnapshotService(DigitalEmployeeSkillMapper employeeSkillMapper, SkillMapper skillMapper,
                                  SkillKnowledgeBaseMapper skillKbMapper, KnowledgeBaseMapper knowledgeBaseMapper,
                                  ObjectMapper objectMapper) {
        this.employeeSkillMapper = employeeSkillMapper;
        this.skillMapper = skillMapper;
        this.skillKbMapper = skillKbMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 加载并校验数字员工关联 Skills（存在且可用），返回快照数据。
     *
     * @param employeeId 数字员工 ID
     * @return Skill 快照列表
     */
    public List<Map<String, Object>> loadSkillSnapshots(Long employeeId) {
        List<Long> skillIds = employeeSkillMapper.selectList(new LambdaQueryWrapper<DigitalEmployeeSkill>()
                        .eq(DigitalEmployeeSkill::getDigitalEmployeeId, employeeId))
                .stream().map(DigitalEmployeeSkill::getSkillId).distinct().toList();
        if (skillIds.isEmpty()) {
            throw new BizException(ErrorCode.DIGITAL_EMPLOYEE_NOT_PUBLISHABLE, "数字员工未关联任何 Skill");
        }
        List<Map<String, Object>> skills = new ArrayList<>();
        for (Long skillId : skillIds) {
            Skill skill = skillMapper.selectById(skillId);
            if (skill == null || skill.getStatus() != SkillStatus.ACTIVE) {
                throw new BizException(ErrorCode.DIGITAL_EMPLOYEE_NOT_PUBLISHABLE,
                        "关联 Skill 不可用: " + skillId);
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("skillId", skill.getId());
            item.put("skillVersion", skill.getVersion());
            skills.add(item);
        }
        return skills;
    }

    /**
     * 展开 Skills 关联知识库并校验 READY，返回快照数据。
     *
     * @param skills Skill 快照列表
     * @return 知识库快照列表
     */
    public List<Map<String, Object>> expandKnowledgeBases(List<Map<String, Object>> skills) {
        Map<Long, KnowledgeBase> readyBases = new LinkedHashMap<>();
        for (Map<String, Object> skill : skills) {
            long skillId = (Long) skill.get("skillId");
            List<Long> kbIds = skillKbMapper.selectList(new LambdaQueryWrapper<SkillKnowledgeBase>()
                            .eq(SkillKnowledgeBase::getSkillId, skillId))
                    .stream().map(SkillKnowledgeBase::getKnowledgeBaseId).toList();
            for (Long kbId : kbIds) {
                if (readyBases.containsKey(kbId)) {
                    continue;
                }
                KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
                if (kb == null || kb.getStatus() != KnowledgeBaseStatus.READY) {
                    throw new BizException(ErrorCode.DIGITAL_EMPLOYEE_NOT_PUBLISHABLE,
                            "关联知识库尚未准备完成: " + kbId);
                }
                readyBases.put(kbId, kb);
            }
        }
        if (readyBases.isEmpty()) {
            throw new BizException(ErrorCode.DIGITAL_EMPLOYEE_NOT_PUBLISHABLE,
                    "Skills 未关联任何可用知识库");
        }
        List<Map<String, Object>> bases = new ArrayList<>();
        readyBases.forEach((kbId, kb) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("knowledgeBaseId", kb.getId());
            item.put("embeddingModel", kb.getEmbeddingModel());
            item.put("chunkCount", kb.getChunkCount());
            bases.add(item);
        });
        return bases;
    }

    /**
     * 构建发布配置快照 JSON。
     *
     * @param systemPrompt     系统提示词
     * @param modelConfigJson  模型配置 JSON
     * @param skills           Skill 快照
     * @param knowledgeBases   知识库快照
     * @return 快照 JSON
     */
    public String buildSnapshot(String systemPrompt, String modelConfigJson,
                                List<Map<String, Object>> skills, List<Map<String, Object>> knowledgeBases) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("systemPrompt", systemPrompt);
        snapshot.put("modelConfig", parseModelConfig(modelConfigJson));
        snapshot.put("skills", skills);
        snapshot.put("knowledgeBases", knowledgeBases);
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception ex) {
            throw new IllegalStateException("发布快照序列化失败", ex);
        }
    }

    /**
     * 校验历史快照引用的 Skills 与知识库仍存在且可用（回滚前置校验）。
     *
     * @param snapshotJson 快照 JSON
     */
    public void validateSnapshotResources(String snapshotJson) {
        try {
            JsonNode root = objectMapper.readTree(snapshotJson);
            for (JsonNode skill : root.path("skills")) {
                long skillId = skill.path("skillId").asLong();
                Skill s = skillMapper.selectById(skillId);
                if (s == null || s.getStatus() != SkillStatus.ACTIVE) {
                    throw new BizException(ErrorCode.DIGITAL_EMPLOYEE_NOT_PUBLISHABLE,
                            "历史版本引用的 Skill 不可用: " + skillId);
                }
            }
            for (JsonNode kb : root.path("knowledgeBases")) {
                long kbId = kb.path("knowledgeBaseId").asLong();
                KnowledgeBase base = knowledgeBaseMapper.selectById(kbId);
                if (base == null || base.getStatus() != KnowledgeBaseStatus.READY) {
                    throw new BizException(ErrorCode.DIGITAL_EMPLOYEE_NOT_PUBLISHABLE,
                            "历史版本引用的知识库不可用: " + kbId);
                }
            }
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("发布快照解析失败", ex);
        }
    }

    /**
     * 解析模型配置 JSON。
     *
     * @param json JSON 字符串
     * @return 模型配置
     */
    public Map<String, Object> parseModelConfig(String json) {
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructMapType(
                    LinkedHashMap.class, String.class, Object.class));
        } catch (Exception ex) {
            return Map.of();
        }
    }
}
