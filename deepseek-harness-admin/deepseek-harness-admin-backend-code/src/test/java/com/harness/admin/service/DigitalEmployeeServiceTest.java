package com.harness.admin.service;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 文件说明：数字员工服务单元测试。
 *
 * <p>覆盖 Skill 可用性校验（成功/缺失/停用）、Skill 去重、模型配置 JSON 序列化、
 * 创建审计字段、列表与详情查询。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@ExtendWith(MockitoExtension.class)
class DigitalEmployeeServiceTest {

    @Mock
    private DigitalEmployeeMapper employeeMapper;

    @Mock
    private DigitalEmployeeSkillMapper associationMapper;

    @Mock
    private SkillMapper skillMapper;

    private DigitalEmployeeService service;

    @BeforeEach
    void setUp() {
        service = new DigitalEmployeeService(employeeMapper, associationMapper, skillMapper, new ObjectMapper());
        AuthContext.set(new UserPrincipal(1L, "admin", UserRole.ADMIN));
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void create_withActiveSkills_shouldSucceed() {
        when(skillMapper.selectById(2L)).thenReturn(skillWithStatus(2L, SkillStatus.ACTIVE));
        when(employeeMapper.insert(any(DigitalEmployee.class))).thenReturn(1);
        Map<String, Object> modelConfig = Map.of("model", "configured-model");

        DigitalEmployeeResponse response = service.create(new DigitalEmployeeRequest(
                "售后助手", "处理售后", "你是售后助手", List.of(2L), modelConfig));

        assertThat(response.name()).isEqualTo("售后助手");
        assertThat(response.status()).isEqualTo(DigitalEmployeeStatus.DRAFT);
        assertThat(response.skillIds()).containsExactly(2L);
        assertThat(response.modelConfig()).containsEntry("model", "configured-model");
        ArgumentCaptor<DigitalEmployee> captor = ArgumentCaptor.forClass(DigitalEmployee.class);
        verify(employeeMapper).insert(captor.capture());
        // 审计字段取自认证上下文，模型配置以 JSON 持久化
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(1L);
        assertThat(captor.getValue().getModelConfig()).contains("\"model\"");
        verify(associationMapper, times(1)).insert(any(DigitalEmployeeSkill.class));
    }

    @Test
    void create_withMissingSkill_shouldReject() {
        when(skillMapper.selectById(9L)).thenReturn(null);

        assertThatThrownBy(() -> service.create(new DigitalEmployeeRequest(
                "助手", "", "提示词", List.of(9L), Map.of("model", "m"))))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        verify(employeeMapper, never()).insert(any(DigitalEmployee.class));
        verify(associationMapper, never()).insert(any(DigitalEmployeeSkill.class));
    }

    @Test
    void create_withDisabledSkill_shouldReject() {
        when(skillMapper.selectById(2L)).thenReturn(skillWithStatus(2L, SkillStatus.DISABLED));

        assertThatThrownBy(() -> service.create(new DigitalEmployeeRequest(
                "助手", "", "提示词", List.of(2L), Map.of("model", "m"))))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void create_duplicateSkillIds_shouldDedupe() {
        when(skillMapper.selectById(2L)).thenReturn(skillWithStatus(2L, SkillStatus.ACTIVE));
        when(employeeMapper.insert(any(DigitalEmployee.class))).thenReturn(1);

        DigitalEmployeeResponse response = service.create(new DigitalEmployeeRequest(
                "助手", "", "提示词", List.of(2L, 2L), Map.of("model", "m")));

        assertThat(response.skillIds()).containsExactly(2L);
        verify(associationMapper, times(1)).insert(any(DigitalEmployeeSkill.class));
    }

    @Test
    void list_shouldReturnPagedEmployeesWithAssociations() {
        DigitalEmployee employee = new DigitalEmployee();
        employee.setId(1L);
        employee.setName("员工A");
        employee.setModelConfig("{\"model\":\"m\"}");
        employee.setStatus(DigitalEmployeeStatus.DRAFT);
        Page<DigitalEmployee> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(employee));
        when(employeeMapper.selectPage(any(), any())).thenReturn(page);
        DigitalEmployeeSkill association = new DigitalEmployeeSkill();
        association.setDigitalEmployeeId(1L);
        association.setSkillId(3L);
        when(associationMapper.selectList(any())).thenReturn(List.of(association));

        PageResult<DigitalEmployeeResponse> result = service.list(1, 10);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).skillIds()).containsExactly(3L);
    }

    @Test
    void detail_notFound_shouldThrowResourceNotFound() {
        when(employeeMapper.selectById(9L)).thenReturn(null);

        assertThatThrownBy(() -> service.detail(9L))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
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
}
