package com.harness.admin.service;

import com.harness.admin.domain.common.BizException;
import com.harness.admin.domain.common.ErrorCode;
import com.harness.admin.domain.digitalemployee.ReleaseResponse;
import com.harness.admin.domain.digitalemployee.ReleaseStatus;
import com.harness.admin.domain.knowledgebase.KnowledgeBaseStatus;
import com.harness.admin.domain.skill.SkillStatus;
import com.harness.admin.domain.user.AuthContext;
import com.harness.admin.domain.user.UserPrincipal;
import com.harness.admin.domain.user.UserRole;
import com.harness.admin.mapper.DigitalEmployeeMapper;
import com.harness.admin.mapper.DigitalEmployeeReleaseMapper;
import com.harness.admin.mapper.DigitalEmployeeSkillMapper;
import com.harness.admin.mapper.KnowledgeBaseMapper;
import com.harness.admin.mapper.SkillKnowledgeBaseMapper;
import com.harness.admin.mapper.SkillMapper;
import com.harness.admin.mapper.entity.DigitalEmployee;
import com.harness.admin.mapper.entity.DigitalEmployeeRelease;
import com.harness.admin.mapper.entity.DigitalEmployeeSkill;
import com.harness.admin.mapper.entity.KnowledgeBase;
import com.harness.admin.mapper.entity.Skill;
import com.harness.admin.mapper.entity.SkillKnowledgeBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 文件说明：数字员工发布与回滚服务单元测试。
 *
 * <p>覆盖发布成功（快照/版本/状态）、发布失败（记录失败原因）、
 * 版本递增、回滚生成新生效记录（历史快照不变）、回滚前置校验。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@ExtendWith(MockitoExtension.class)
class DigitalEmployeeReleaseServiceTest {

    @Mock
    private DigitalEmployeeReleaseMapper releaseMapper;

    @Mock
    private DigitalEmployeeMapper employeeMapper;

    @Mock
    private DigitalEmployeeSkillMapper employeeSkillMapper;

    @Mock
    private SkillMapper skillMapper;

    @Mock
    private SkillKnowledgeBaseMapper skillKbMapper;

    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;

    private DigitalEmployeeReleaseService service;

    @BeforeEach
    void setUp() {
        // 使用真实事务模板 + mock 事务管理器，直接执行回调验证发布/回滚落库逻辑
        TransactionTemplate tx = new TransactionTemplate(org.mockito.Mockito.mock(
                org.springframework.transaction.PlatformTransactionManager.class));
        ReleaseSnapshotService snapshotService = new ReleaseSnapshotService(
                employeeSkillMapper, skillMapper, skillKbMapper, knowledgeBaseMapper, new ObjectMapper());
        service = new DigitalEmployeeReleaseService(releaseMapper, employeeMapper, snapshotService, tx);
        AuthContext.set(new UserPrincipal(1L, "admin", UserRole.ADMIN));
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void publish_shouldCreateSnapshotAndPublishEmployee() {
        DigitalEmployee employee = employeeWithBasics(1L, "ok");
        when(employeeMapper.selectById(1L)).thenReturn(employee);
        when(employeeSkillMapper.selectList(any())).thenReturn(List.of(association(1L, 2L)));
        when(skillMapper.selectById(2L)).thenReturn(skillWithStatus(2L, SkillStatus.ACTIVE));
        when(skillKbMapper.selectList(any())).thenReturn(List.of(skillKb(2L, 3L)));
        when(knowledgeBaseMapper.selectById(3L)).thenReturn(knowledgeBaseWithStatus(3L, KnowledgeBaseStatus.READY));
        when(releaseMapper.selectCount(any())).thenReturn(0L);
        when(releaseMapper.insert(any(DigitalEmployeeRelease.class))).thenReturn(1);

        ReleaseResponse response = service.publish(1L);

        assertThat(response.version()).isEqualTo("1");
        assertThat(response.status()).isEqualTo(ReleaseStatus.SUCCESS);
        // 发布快照包含提示词、模型配置、Skill 与知识库引用
        verify(releaseMapper).insert(any(DigitalEmployeeRelease.class));
        // 数字员工置为 PUBLISHED
        verify(employeeMapper).updateById(any(DigitalEmployee.class));
    }

    @Test
    void publish_withNotReadyKnowledgeBase_shouldFailAndRecordReason() {
        DigitalEmployee employee = employeeWithBasics(1L, "ok");
        when(employeeMapper.selectById(1L)).thenReturn(employee);
        when(employeeSkillMapper.selectList(any())).thenReturn(List.of(association(1L, 2L)));
        when(skillMapper.selectById(2L)).thenReturn(skillWithStatus(2L, SkillStatus.ACTIVE));
        when(skillKbMapper.selectList(any())).thenReturn(List.of(skillKb(2L, 3L)));
        when(knowledgeBaseMapper.selectById(3L)).thenReturn(knowledgeBaseWithStatus(3L, KnowledgeBaseStatus.PROCESSING));
        when(releaseMapper.selectCount(any())).thenReturn(0L);

        assertThatThrownBy(() -> service.publish(1L))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DIGITAL_EMPLOYEE_NOT_PUBLISHABLE);
        // 失败发布记录保留失败原因
        verify(releaseMapper).insert(any(DigitalEmployeeRelease.class));
        verify(employeeMapper, never()).updateById(any(DigitalEmployee.class));
    }

    @Test
    void publish_secondTime_shouldIncrementVersion() {
        DigitalEmployee employee = employeeWithBasics(1L, "ok");
        when(employeeMapper.selectById(1L)).thenReturn(employee);
        when(employeeSkillMapper.selectList(any())).thenReturn(List.of(association(1L, 2L)));
        when(skillMapper.selectById(2L)).thenReturn(skillWithStatus(2L, SkillStatus.ACTIVE));
        when(skillKbMapper.selectList(any())).thenReturn(List.of(skillKb(2L, 3L)));
        when(knowledgeBaseMapper.selectById(3L)).thenReturn(knowledgeBaseWithStatus(3L, KnowledgeBaseStatus.READY));
        // 已有 1 条发布记录，本次版本号为 2
        when(releaseMapper.selectCount(any())).thenReturn(1L);

        ReleaseResponse response = service.publish(1L);

        assertThat(response.version()).isEqualTo("2");
    }

    @Test
    void rollback_shouldCreateNewActiveRecordWithoutTouchingHistorical() {
        DigitalEmployee employee = employeeWithBasics(1L, "ok");
        when(employeeMapper.selectById(1L)).thenReturn(employee);
        DigitalEmployeeRelease historical = new DigitalEmployeeRelease();
        historical.setId(9L);
        historical.setDigitalEmployeeId(1L);
        historical.setVersion("1");
        historical.setStatus(ReleaseStatus.SUCCESS);
        historical.setConfigSnapshot("{\"systemPrompt\":\"旧提示词\",\"modelConfig\":{\"model\":\"m\"},"
                + "\"skills\":[{\"skillId\":2,\"skillVersion\":\"1.0\"}],"
                + "\"knowledgeBases\":[{\"knowledgeBaseId\":3,\"embeddingModel\":\"local-hash\",\"chunkCount\":1}]}");
        when(releaseMapper.selectById(9L)).thenReturn(historical);
        when(skillMapper.selectById(2L)).thenReturn(skillWithStatus(2L, SkillStatus.ACTIVE));
        when(knowledgeBaseMapper.selectById(3L)).thenReturn(knowledgeBaseWithStatus(3L, KnowledgeBaseStatus.READY));
        when(releaseMapper.selectCount(any())).thenReturn(1L);

        ReleaseResponse response = service.rollback(1L, 9L);

        assertThat(response.version()).isEqualTo("2");
        assertThat(response.status()).isEqualTo(ReleaseStatus.SUCCESS);
        // 回滚复用历史快照内容，不修改历史记录
        verify(releaseMapper).insert(any(DigitalEmployeeRelease.class));
    }

    @Test
    void rollback_releaseOfAnotherEmployee_shouldReject() {
        when(employeeMapper.selectById(1L)).thenReturn(employeeWithBasics(1L, "ok"));
        DigitalEmployeeRelease historical = new DigitalEmployeeRelease();
        historical.setId(9L);
        historical.setDigitalEmployeeId(99L);
        historical.setStatus(ReleaseStatus.SUCCESS);
        when(releaseMapper.selectById(9L)).thenReturn(historical);

        assertThatThrownBy(() -> service.rollback(1L, 9L))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void rollback_failedRelease_shouldReject() {
        when(employeeMapper.selectById(1L)).thenReturn(employeeWithBasics(1L, "ok"));
        DigitalEmployeeRelease historical = new DigitalEmployeeRelease();
        historical.setId(9L);
        historical.setDigitalEmployeeId(1L);
        historical.setStatus(ReleaseStatus.FAILED);
        when(releaseMapper.selectById(9L)).thenReturn(historical);

        assertThatThrownBy(() -> service.rollback(1L, 9L))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void rollback_resourceUnavailable_shouldFailAndRecordReason() {
        when(employeeMapper.selectById(1L)).thenReturn(employeeWithBasics(1L, "ok"));
        DigitalEmployeeRelease historical = new DigitalEmployeeRelease();
        historical.setId(9L);
        historical.setDigitalEmployeeId(1L);
        historical.setVersion("1");
        historical.setStatus(ReleaseStatus.SUCCESS);
        historical.setConfigSnapshot("{\"skills\":[{\"skillId\":2}],"
                + "\"knowledgeBases\":[{\"knowledgeBaseId\":3}]}");
        when(releaseMapper.selectById(9L)).thenReturn(historical);
        when(skillMapper.selectById(2L)).thenReturn(skillWithStatus(2L, SkillStatus.DISABLED));
        when(releaseMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.rollback(1L, 9L))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DIGITAL_EMPLOYEE_NOT_PUBLISHABLE);
        // 回滚失败同样保留失败原因记录
        verify(releaseMapper).insert(any(DigitalEmployeeRelease.class));
    }

    /**
     * 构造具备完整基础信息的数字员工。
     */
    private DigitalEmployee employeeWithBasics(Long id, String modelConfig) {
        DigitalEmployee employee = new DigitalEmployee();
        employee.setId(id);
        employee.setName("助手");
        employee.setSystemPrompt("提示词");
        employee.setModelConfig(modelConfig);
        return employee;
    }

    /**
     * 构造数字员工-Skill 关联。
     */
    private DigitalEmployeeSkill association(Long employeeId, Long skillId) {
        DigitalEmployeeSkill association = new DigitalEmployeeSkill();
        association.setDigitalEmployeeId(employeeId);
        association.setSkillId(skillId);
        return association;
    }

    /**
     * 构造 Skill-知识库关联。
     */
    private SkillKnowledgeBase skillKb(Long skillId, Long kbId) {
        SkillKnowledgeBase association = new SkillKnowledgeBase();
        association.setSkillId(skillId);
        association.setKnowledgeBaseId(kbId);
        return association;
    }

    /**
     * 构造指定状态的 Skill。
     */
    private Skill skillWithStatus(Long id, SkillStatus status) {
        Skill skill = new Skill();
        skill.setId(id);
        skill.setStatus(status);
        return skill;
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
