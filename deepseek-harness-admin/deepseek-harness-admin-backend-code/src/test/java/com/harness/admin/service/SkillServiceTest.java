package com.harness.admin.service;

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
import com.harness.admin.domain.user.UserRole;
import com.harness.admin.mapper.KnowledgeBaseMapper;
import com.harness.admin.mapper.SkillKnowledgeBaseMapper;
import com.harness.admin.mapper.SkillMapper;
import com.harness.admin.mapper.entity.KnowledgeBase;
import com.harness.admin.mapper.entity.Skill;
import com.harness.admin.mapper.entity.SkillKnowledgeBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 文件说明：Skill 服务单元测试。
 *
 * <p>覆盖关联 READY 知识库校验（成功/非 READY/不存在）、知识库去重、
 * 创建审计字段、列表与详情查询。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock
    private SkillMapper skillMapper;

    @Mock
    private SkillKnowledgeBaseMapper associationMapper;

    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;

    private SkillService service;

    @BeforeEach
    void setUp() {
        service = new SkillService(skillMapper, associationMapper, knowledgeBaseMapper);
        AuthContext.set(new UserPrincipal(1L, "admin", UserRole.ADMIN));
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void create_withReadyKnowledgeBases_shouldSucceed() {
        KnowledgeBase ready = knowledgeBaseWithStatus(2L, KnowledgeBaseStatus.READY);
        KnowledgeBase ready2 = knowledgeBaseWithStatus(3L, KnowledgeBaseStatus.READY);
        when(knowledgeBaseMapper.selectById(2L)).thenReturn(ready);
        when(knowledgeBaseMapper.selectById(3L)).thenReturn(ready2);
        when(skillMapper.insert(any(Skill.class))).thenReturn(1);

        SkillResponse response = service.create(new SkillRequest("客服助手", "处理咨询", "内容配置",
                List.of(2L, 3L)));

        assertThat(response.name()).isEqualTo("客服助手");
        assertThat(response.status()).isEqualTo(SkillStatus.ACTIVE);
        assertThat(response.version()).isEqualTo("1.0");
        assertThat(response.knowledgeBaseIds()).containsExactly(2L, 3L);
        ArgumentCaptor<Skill> skillCaptor = ArgumentCaptor.forClass(Skill.class);
        verify(skillMapper).insert(skillCaptor.capture());
        // 审计字段取自认证上下文
        assertThat(skillCaptor.getValue().getCreatedBy()).isEqualTo(1L);
        // 两条关联均写入
        ArgumentCaptor<SkillKnowledgeBase> assocCaptor = ArgumentCaptor.forClass(SkillKnowledgeBase.class);
        verify(associationMapper, times(2)).insert(assocCaptor.capture());
    }

    @Test
    void create_withNotReadyKnowledgeBase_shouldReject() {
        when(knowledgeBaseMapper.selectById(2L)).thenReturn(knowledgeBaseWithStatus(2L, KnowledgeBaseStatus.PROCESSING));

        assertThatThrownBy(() -> service.create(new SkillRequest("技能", "", "内容", List.of(2L))))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.KNOWLEDGE_BASE_NOT_READY);
        // 校验失败不得写入 Skill
        verify(skillMapper, never()).insert(any(Skill.class));
        verify(associationMapper, never()).insert(any(SkillKnowledgeBase.class));
    }

    @Test
    void create_withMissingKnowledgeBase_shouldReject() {
        when(knowledgeBaseMapper.selectById(9L)).thenReturn(null);

        assertThatThrownBy(() -> service.create(new SkillRequest("技能", "", "内容", List.of(9L))))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void create_duplicateKnowledgeBaseIds_shouldDedupe() {
        KnowledgeBase ready = knowledgeBaseWithStatus(2L, KnowledgeBaseStatus.READY);
        when(knowledgeBaseMapper.selectById(2L)).thenReturn(ready);
        when(skillMapper.insert(any(Skill.class))).thenReturn(1);

        SkillResponse response = service.create(new SkillRequest("技能", "", "内容", List.of(2L, 2L)));

        // 重复知识库只保留一条关联
        assertThat(response.knowledgeBaseIds()).containsExactly(2L);
        verify(associationMapper, times(1)).insert(any(SkillKnowledgeBase.class));
    }

    @Test
    void list_shouldReturnPagedSkillsWithAssociations() {
        Skill skill = new Skill();
        skill.setId(1L);
        skill.setName("技能A");
        skill.setStatus(SkillStatus.ACTIVE);
        Page<Skill> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(skill));
        when(skillMapper.selectPage(any(), any())).thenReturn(page);
        SkillKnowledgeBase association = new SkillKnowledgeBase();
        association.setSkillId(1L);
        association.setKnowledgeBaseId(5L);
        when(associationMapper.selectList(any())).thenReturn(List.of(association));

        PageResult<SkillResponse> result = service.list(1, 10);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).knowledgeBaseIds()).containsExactly(5L);
    }

    @Test
    void detail_notFound_shouldThrowResourceNotFound() {
        when(skillMapper.selectById(9L)).thenReturn(null);

        assertThatThrownBy(() -> service.detail(9L))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    /**
     * 构造指定状态的 KnowledgeBase。
     */
    private KnowledgeBase knowledgeBaseWithStatus(Long id, KnowledgeBaseStatus status) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(id);
        kb.setStatus(status);
        return kb;
    }
}
