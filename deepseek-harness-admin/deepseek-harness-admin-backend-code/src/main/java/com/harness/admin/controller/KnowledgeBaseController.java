package com.harness.admin.controller;

import com.harness.admin.config.RequireAdmin;
import com.harness.admin.domain.common.ApiResponse;
import com.harness.admin.domain.common.PageResult;
import com.harness.admin.domain.knowledgebase.KnowledgeBaseResponse;
import com.harness.admin.service.KnowledgeBaseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件说明：知识库管理接口。
 *
 * <p>上传、列表、详情均由管理员使用，全局共享资源不按创建人过滤；
 * 控制器仅做参数接收与响应转换，业务校验在 Service 层。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@RestController
@RequestMapping("/api/knowledge-bases")
@RequireAdmin
public class KnowledgeBaseController {

    /** 知识库服务 */
    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 构造知识库控制器。
     *
     * @param knowledgeBaseService 知识库服务
     */
    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    /**
     * 上传知识库文件（multipart/form-data）。
     *
     * @param file 上传文件
     * @param name 知识库名称（可选）
     * @return 知识库信息（UPLOADING 状态）
     */
    @PostMapping
    public ApiResponse<KnowledgeBaseResponse> upload(@RequestParam("file") MultipartFile file,
                                                     @RequestParam(value = "name", required = false) String name) {
        return ApiResponse.success(knowledgeBaseService.upload(file, name));
    }

    /**
     * 分页查询共享知识库列表。
     *
     * @param page 页码
     * @param size 每页条数
     * @return 分页结果
     */
    @GetMapping
    public ApiResponse<PageResult<KnowledgeBaseResponse>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.success(knowledgeBaseService.list(page, size));
    }

    /**
     * 查询知识库详情。
     *
     * @param id 知识库 ID
     * @return 知识库信息
     */
    @GetMapping("/{id}")
    public ApiResponse<KnowledgeBaseResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(knowledgeBaseService.detail(id));
    }

    /**
     * 重新处理知识库（清理旧向量后重新向量化）。
     *
     * @param id 知识库 ID
     * @return 知识库信息
     */
    @PostMapping("/{id}/reprocess")
    public ApiResponse<KnowledgeBaseResponse> reprocess(@PathVariable Long id) {
        return ApiResponse.success(knowledgeBaseService.reprocess(id));
    }
}
