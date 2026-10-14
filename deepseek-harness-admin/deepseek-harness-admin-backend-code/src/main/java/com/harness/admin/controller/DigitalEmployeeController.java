package com.harness.admin.controller;

import com.harness.admin.config.RequireAdmin;
import com.harness.admin.domain.common.ApiResponse;
import com.harness.admin.domain.common.PageResult;
import com.harness.admin.domain.digitalemployee.DigitalEmployeeRequest;
import com.harness.admin.domain.digitalemployee.DigitalEmployeeResponse;
import com.harness.admin.domain.digitalemployee.ReleaseResponse;
import com.harness.admin.domain.digitalemployee.RollbackRequest;
import com.harness.admin.domain.user.AuthContext;
import com.harness.admin.domain.user.UserPrincipal;
import com.harness.admin.domain.user.UserRole;
import com.harness.admin.service.DigitalEmployeeReleaseService;
import com.harness.admin.service.DigitalEmployeeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 文件说明：数字员工接口。
 *
 * <p>浏览与对话对已登录用户开放（普通用户仅可见已发布数字员工，内部配置在响应边界裁剪）；
 * 创建、发布、回滚等管理操作要求管理员角色。请求体不接收知识库 ID，
 * 数字员工的知识库由所选 Skills 间接确定。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@RestController
@RequestMapping("/api/digital-employees")
public class DigitalEmployeeController {

    /** 数字员工服务 */
    private final DigitalEmployeeService digitalEmployeeService;

    /** 发布与回滚服务 */
    private final DigitalEmployeeReleaseService releaseService;

    /**
     * 构造数字员工控制器。
     *
     * @param digitalEmployeeService 数字员工服务
     * @param releaseService         发布与回滚服务
     */
    public DigitalEmployeeController(DigitalEmployeeService digitalEmployeeService,
                                     DigitalEmployeeReleaseService releaseService) {
        this.digitalEmployeeService = digitalEmployeeService;
        this.releaseService = releaseService;
    }

    /**
     * 创建数字员工（管理操作）。
     *
     * @param request 创建请求
     * @return 数字员工信息
     */
    @PostMapping
    @RequireAdmin
    public ApiResponse<DigitalEmployeeResponse> create(@Valid @RequestBody DigitalEmployeeRequest request) {
        return ApiResponse.success(digitalEmployeeService.create(request));
    }

    /**
     * 分页查询数字员工列表。
     *
     * <p>普通用户仅返回已发布数字员工，且响应字段按 {@code toUserView} 裁剪。</p>
     *
     * @param page 页码
     * @param size 每页条数
     * @return 分页结果
     */
    @GetMapping
    public ApiResponse<PageResult<DigitalEmployeeResponse>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        PageResult<DigitalEmployeeResponse> result = digitalEmployeeService.list(page, size);
        return ApiResponse.success(applyUserView(result));
    }

    /**
     * 查询数字员工详情。
     *
     * <p>普通用户仅可查看已发布数字员工，且响应字段按 {@code toUserView} 裁剪。</p>
     *
     * @param id 数字员工 ID
     * @return 数字员工信息
     */
    @GetMapping("/{id}")
    public ApiResponse<DigitalEmployeeResponse> detail(@PathVariable Long id) {
        DigitalEmployeeResponse response = digitalEmployeeService.detail(id);
        return ApiResponse.success(applyUserView(response));
    }

    /**
     * 发布数字员工：校验通过后生成不可变版本快照（管理操作）。
     *
     * @param id 数字员工 ID
     * @return 发布记录
     */
    @PostMapping("/{id}/publish")
    @RequireAdmin
    public ApiResponse<ReleaseResponse> publish(@PathVariable Long id) {
        return ApiResponse.success(releaseService.publish(id));
    }

    /**
     * 回滚到历史发布版本：生成新的生效记录，不改历史快照（管理操作）。
     *
     * @param id      数字员工 ID
     * @param request 回滚请求（历史发布记录 ID）
     * @return 新的发布记录
     */
    @PostMapping("/{id}/rollback")
    @RequireAdmin
    public ApiResponse<ReleaseResponse> rollback(@PathVariable Long id,
                                                 @Valid @RequestBody RollbackRequest request) {
        return ApiResponse.success(releaseService.rollback(id, request.releaseId()));
    }

    /**
     * 普通用户响应裁剪：隐藏系统提示词、模型配置、发布版本与 Skill 关联等内部配置；
     * 管理员返回全量。
     *
     * @param response 数字员工响应
     * @return 裁剪后的响应
     */
    private DigitalEmployeeResponse applyUserView(DigitalEmployeeResponse response) {
        return isUser() ? response.toUserView() : response;
    }

    /**
     * 分页结果整体执行普通用户响应裁剪。
     *
     * @param result 分页结果
     * @return 裁剪后的分页结果
     */
    private PageResult<DigitalEmployeeResponse> applyUserView(PageResult<DigitalEmployeeResponse> result) {
        if (!isUser()) {
            return result;
        }
        List<DigitalEmployeeResponse> items = result.items().stream()
                .map(DigitalEmployeeResponse::toUserView)
                .toList();
        return new PageResult<>(result.page(), result.size(), result.total(), items);
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
