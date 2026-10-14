package com.harness.admin.controller;

import com.harness.admin.config.RequireAdmin;
import com.harness.admin.domain.common.ApiResponse;
import com.harness.admin.domain.common.PageResult;
import com.harness.admin.domain.skill.SkillRequest;
import com.harness.admin.domain.skill.SkillResponse;
import com.harness.admin.service.SkillService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件说明：Skill 管理接口。
 *
 * <p>创建、列表、详情均要求管理员角色；关联校验与业务规则在 Service 层。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@RestController
@RequestMapping("/api/skills")
@RequireAdmin
public class SkillController {

    /** Skill 服务 */
    private final SkillService skillService;

    /**
     * 构造 Skill 控制器。
     *
     * @param skillService Skill 服务
     */
    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    /**
     * 创建 Skill。
     *
     * @param request 创建请求
     * @return Skill 信息
     */
    @PostMapping
    public ApiResponse<SkillResponse> create(@Valid @RequestBody SkillRequest request) {
        return ApiResponse.success(skillService.create(request));
    }

    /**
     * 分页查询 Skill 列表。
     *
     * @param page 页码
     * @param size 每页条数
     * @return 分页结果
     */
    @GetMapping
    public ApiResponse<PageResult<SkillResponse>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.success(skillService.list(page, size));
    }

    /**
     * 查询 Skill 详情。
     *
     * @param id Skill ID
     * @return Skill 信息
     */
    @GetMapping("/{id}")
    public ApiResponse<SkillResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(skillService.detail(id));
    }
}
