package com.harness.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harness.admin.domain.common.BizException;
import com.harness.admin.domain.common.ErrorCode;
import com.harness.admin.domain.digitalemployee.DigitalEmployeeStatus;
import com.harness.admin.domain.digitalemployee.ReleaseResponse;
import com.harness.admin.domain.digitalemployee.ReleaseStatus;
import com.harness.admin.domain.user.AuthContext;
import com.harness.admin.domain.user.UserPrincipal;
import com.harness.admin.mapper.DigitalEmployeeMapper;
import com.harness.admin.mapper.DigitalEmployeeReleaseMapper;
import com.harness.admin.mapper.entity.DigitalEmployee;
import com.harness.admin.mapper.entity.DigitalEmployeeRelease;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 文件说明：数字员工发布与回滚服务。
 *
 * <p>发布前校验基础信息、Skills 可用性并展开校验知识库 READY 状态（快照构建见
 * {@link ReleaseSnapshotService}）；成功后生成不可变配置快照与版本号。
 * 发布失败保留失败原因记录；回滚基于历史快照生成新的生效记录，不改历史快照。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Service
public class DigitalEmployeeReleaseService {

    /** 结构化日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(DigitalEmployeeReleaseService.class);

    /** 发布记录表数据访问 */
    private final DigitalEmployeeReleaseMapper releaseMapper;

    /** 数字员工表数据访问 */
    private final DigitalEmployeeMapper employeeMapper;

    /** 发布快照构建与校验 */
    private final ReleaseSnapshotService snapshotService;

    /** 编程式事务模板（快照记录与数字员工状态更新原子提交） */
    private final TransactionTemplate transactionTemplate;

    /**
     * 构造发布与回滚服务。
     *
     * @param releaseMapper       发布记录表数据访问
     * @param employeeMapper      数字员工表数据访问
     * @param snapshotService     发布快照服务
     * @param transactionTemplate 事务模板
     */
    public DigitalEmployeeReleaseService(DigitalEmployeeReleaseMapper releaseMapper,
                                         DigitalEmployeeMapper employeeMapper,
                                         ReleaseSnapshotService snapshotService,
                                         TransactionTemplate transactionTemplate) {
        this.releaseMapper = releaseMapper;
        this.employeeMapper = employeeMapper;
        this.snapshotService = snapshotService;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 发布数字员工：校验通过后生成不可变快照并更新发布状态。
     *
     * @param employeeId 数字员工 ID
     * @return 发布记录
     */
    public ReleaseResponse publish(Long employeeId) {
        UserPrincipal operator = AuthContext.get();
        if (operator == null) {
            throw new BizException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        DigitalEmployee employee = loadEmployee(employeeId);
        try {
            validateBasics(employee);
            List<Map<String, Object>> skills = snapshotService.loadSkillSnapshots(employeeId);
            List<Map<String, Object>> knowledgeBases = snapshotService.expandKnowledgeBases(skills);
            String snapshot = snapshotService.buildSnapshot(employee.getSystemPrompt(),
                    employee.getModelConfig(), skills, knowledgeBases);
            DigitalEmployeeRelease release = createRelease(employeeId, snapshot, operator.id());
            log.info("digital employee published, employeeId={}, version={}, releasedBy={}",
                    employeeId, release.getVersion(), operator.id());
            return toResponse(release);
        } catch (BizException ex) {
            // 发布失败保留失败原因记录（独立事务，避免随异常回滚）
            createFailedRelease(employeeId, ex.getMessage(), operator.id());
            throw ex;
        }
    }

    /**
     * 回滚到历史发布版本：基于所选快照生成新的生效记录，不改历史快照。
     *
     * @param employeeId 数字员工 ID
     * @param releaseId  历史发布记录 ID
     * @return 新的发布记录
     */
    public ReleaseResponse rollback(Long employeeId, Long releaseId) {
        UserPrincipal operator = AuthContext.get();
        if (operator == null) {
            throw new BizException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        loadEmployee(employeeId);
        DigitalEmployeeRelease historical = releaseMapper.selectById(releaseId);
        if (historical == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "发布记录不存在");
        }
        if (!employeeId.equals(historical.getDigitalEmployeeId())) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "发布记录不属于该数字员工");
        }
        if (historical.getStatus() != ReleaseStatus.SUCCESS) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "仅可回滚成功的发布记录");
        }
        try {
            // 回滚前校验历史快照引用的资源仍存在且可用
            snapshotService.validateSnapshotResources(historical.getConfigSnapshot());
            DigitalEmployeeRelease release = createRelease(employeeId, historical.getConfigSnapshot(), operator.id());
            log.info("digital employee rolled back, employeeId={}, fromReleaseId={}, newVersion={}, releasedBy={}",
                    employeeId, releaseId, release.getVersion(), operator.id());
            return toResponse(release);
        } catch (BizException ex) {
            createFailedRelease(employeeId, ex.getMessage(), operator.id());
            throw ex;
        }
    }

    /**
     * 加载数字员工，不存在时抛出业务错误。
     *
     * @param employeeId 数字员工 ID
     * @return 数字员工
     */
    private DigitalEmployee loadEmployee(Long employeeId) {
        DigitalEmployee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "数字员工不存在");
        }
        return employee;
    }

    /**
     * 校验数字员工基础信息完整（名称、提示词、模型配置）。
     *
     * @param employee 数字员工
     */
    private void validateBasics(DigitalEmployee employee) {
        if (isBlank(employee.getName()) || isBlank(employee.getSystemPrompt())
                || isBlank(employee.getModelConfig())) {
            throw new BizException(ErrorCode.DIGITAL_EMPLOYEE_NOT_PUBLISHABLE, "数字员工基础信息不完整");
        }
    }

    /**
     * 创建成功发布记录并更新数字员工发布状态（同一事务）。
     *
     * @param employeeId 数字员工 ID
     * @param snapshot   配置快照
     * @param releasedBy 发布人
     * @return 发布记录
     */
    private DigitalEmployeeRelease createRelease(Long employeeId, String snapshot, Long releasedBy) {
        return transactionTemplate.execute(status -> {
            DigitalEmployeeRelease release = new DigitalEmployeeRelease();
            release.setDigitalEmployeeId(employeeId);
            release.setVersion(nextVersion(employeeId));
            release.setConfigSnapshot(snapshot);
            release.setReleasedBy(releasedBy);
            release.setReleasedAt(LocalDateTime.now());
            release.setStatus(ReleaseStatus.SUCCESS);
            releaseMapper.insert(release);
            DigitalEmployee employee = employeeMapper.selectById(employeeId);
            employee.setStatus(DigitalEmployeeStatus.PUBLISHED);
            employee.setPublishedVersion(release.getVersion());
            employeeMapper.updateById(employee);
            return release;
        });
    }

    /**
     * 记录一次失败的发布/回滚（独立事务，保留失败原因）。
     *
     * @param employeeId 数字员工 ID
     * @param reason     失败原因
     * @param releasedBy 操作者
     */
    private void createFailedRelease(Long employeeId, String reason, Long releasedBy) {
        transactionTemplate.executeWithoutResult(status -> {
            DigitalEmployeeRelease release = new DigitalEmployeeRelease();
            release.setDigitalEmployeeId(employeeId);
            release.setVersion(nextVersion(employeeId));
            release.setConfigSnapshot("{}");
            release.setReleasedBy(releasedBy);
            release.setReleasedAt(LocalDateTime.now());
            release.setStatus(ReleaseStatus.FAILED);
            release.setFailureReason(shortenReason(reason));
            releaseMapper.insert(release);
        });
    }

    /**
     * 计算下一个版本号（基于该数字员工全部发布记录计数）。
     *
     * @param employeeId 数字员工 ID
     * @return 版本号
     */
    private String nextVersion(Long employeeId) {
        long count = releaseMapper.selectCount(new LambdaQueryWrapper<DigitalEmployeeRelease>()
                .eq(DigitalEmployeeRelease::getDigitalEmployeeId, employeeId));
        return String.valueOf(count + 1);
    }

    /**
     * 截断失败原因。
     *
     * @param message 原始信息
     * @return 截断结果
     */
    private String shortenReason(String message) {
        if (message == null || message.isBlank()) {
            return "未知错误";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    /**
     * 判断文本是否为空。
     *
     * @param value 文本
     * @return 是否为空
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 发布记录转响应模型。
     *
     * @param release 发布记录
     * @return 发布响应
     */
    private ReleaseResponse toResponse(DigitalEmployeeRelease release) {
        return new ReleaseResponse(release.getId(), release.getDigitalEmployeeId(), release.getVersion(),
                release.getStatus(), release.getFailureReason(), release.getReleasedAt());
    }
}
