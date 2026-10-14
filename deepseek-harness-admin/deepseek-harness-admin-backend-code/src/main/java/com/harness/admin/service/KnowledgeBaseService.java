package com.harness.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.harness.admin.config.FileStorageService;
import com.harness.admin.config.StorageProperties;
import com.harness.admin.domain.common.BizException;
import com.harness.admin.domain.common.ErrorCode;
import com.harness.admin.domain.common.PageResult;
import com.harness.admin.domain.knowledgebase.KnowledgeBaseResponse;
import com.harness.admin.domain.knowledgebase.KnowledgeBaseStatus;
import com.harness.admin.domain.user.AuthContext;
import com.harness.admin.domain.user.UserPrincipal;
import com.harness.admin.mapper.KnowledgeBaseMapper;
import com.harness.admin.mapper.entity.KnowledgeBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * 文件说明：知识库服务。
 *
 * <p>职责：上传文件（类型/大小/名称校验）、保存文件与元数据、分页列表与详情查询。
 * 知识库为全局共享资源，不按创建人过滤；操作者取自认证上下文，仅用于审计。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Service
public class KnowledgeBaseService {

    /** 结构化日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    /** 单页大小上限 */
    private static final long MAX_PAGE_SIZE = 100;

    /** 知识库名称最大长度 */
    private static final int MAX_NAME_LENGTH = 128;

    /** 知识库表数据访问 */
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    /** 文件存储服务 */
    private final FileStorageService fileStorageService;

    /** 存储与上传校验配置 */
    private final StorageProperties storageProperties;

    /** 向量化任务治理服务 */
    private final VectorizationTaskService vectorizationTaskService;

    /**
     * 构造知识库服务。
     *
     * @param knowledgeBaseMapper    知识库表数据访问
     * @param fileStorageService     文件存储服务
     * @param storageProperties      存储配置
     * @param vectorizationTaskService 向量化任务治理服务
     */
    public KnowledgeBaseService(KnowledgeBaseMapper knowledgeBaseMapper, FileStorageService fileStorageService,
                                StorageProperties storageProperties,
                                VectorizationTaskService vectorizationTaskService) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.fileStorageService = fileStorageService;
        this.storageProperties = storageProperties;
        this.vectorizationTaskService = vectorizationTaskService;
    }

    /**
     * 上传知识库文件：校验后保存文件并创建 UPLOADING 状态记录。
     *
     * @param file 上传文件
     * @param name 知识库名称（可选，缺省取文件名）
     * @return 知识库信息
     */
    public KnowledgeBaseResponse upload(MultipartFile file, String name) {
        validateFile(file);
        String ext = extensionOf(file.getOriginalFilename());
        UserPrincipal operator = AuthContext.get();
        if (operator == null) {
            // 拦截器应已保证认证，此处仅防御性校验
            throw new BizException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        String displayName = resolveName(name, file.getOriginalFilename());
        // 存储键随机生成，杜绝路径穿越与文件名冲突
        String storageKey = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        fileStorageService.save(file, storageKey);
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(displayName);
        kb.setStatus(KnowledgeBaseStatus.UPLOADING);
        kb.setFileName(sanitizeFileName(file.getOriginalFilename()));
        kb.setStorageKey(storageKey);
        kb.setChunkCount(0);
        kb.setCreatedBy(operator.id());
        try {
            knowledgeBaseMapper.insert(kb);
        } catch (RuntimeException ex) {
            // 元数据入库失败时清理已落盘文件，避免残留
            fileStorageService.delete(storageKey);
            throw ex;
        }
        log.info("knowledge base uploaded, kbId={}, name={}, fileName={}, createdBy={}",
                kb.getId(), kb.getName(), kb.getFileName(), kb.getCreatedBy());
        // 上传成功后异步触发向量化任务（解析、切分、Embedding、向量写入）
        vectorizationTaskService.startFor(kb.getId(), kb.getStorageKey());
        return toResponse(kb);
    }

    /**
     * 重新处理知识库：清理旧向量后重新执行向量化。
     *
     * @param id 知识库 ID
     * @return 知识库信息
     */
    public KnowledgeBaseResponse reprocess(Long id) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id);
        if (kb == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "知识库不存在");
        }
        if (kb.getStatus() == KnowledgeBaseStatus.UPLOADING || kb.getStatus() == KnowledgeBaseStatus.PROCESSING) {
            throw new BizException(ErrorCode.KNOWLEDGE_BASE_PROCESSING, "知识库正在向量化处理");
        }
        // 任务治理内部校验活跃任务并异步执行
        vectorizationTaskService.startFor(kb.getId(), kb.getStorageKey());
        log.info("knowledge base reprocess triggered, kbId={}", kb.getId());
        return toResponse(kb);
    }

    /**
     * 分页查询共享知识库列表（全局共享，不按创建人过滤）。
     *
     * @param page 页码（从 1 开始）
     * @param size 每页条数
     * @return 分页结果
     */
    public PageResult<KnowledgeBaseResponse> list(long page, long size) {
        // 页码与大小做边界收敛，防止一次查询过大
        long safePage = Math.max(page, 1);
        long safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<KnowledgeBase> result = knowledgeBaseMapper.selectPage(new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<KnowledgeBase>().orderByDesc(KnowledgeBase::getCreatedAt));
        List<KnowledgeBaseResponse> items = result.getRecords().stream().map(this::toResponse).toList();
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), items);
    }

    /**
     * 查询知识库详情。
     *
     * @param id 知识库 ID
     * @return 知识库信息
     */
    public KnowledgeBaseResponse detail(Long id) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id);
        if (kb == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "知识库不存在");
        }
        return toResponse(kb);
    }

    /**
     * 校验上传文件：非空、扩展名允许、大小未超限。
     *
     * @param file 上传文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "上传文件不能为空");
        }
        String ext = extensionOf(file.getOriginalFilename());
        if (ext.isEmpty() || !storageProperties.getAllowedExtensions().contains(ext)) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "不支持的文件类型");
        }
        long maxBytes = storageProperties.getMaxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "文件大小超过上限");
        }
    }

    /**
     * 解析文件扩展名（小写）。
     *
     * @param originalFilename 原始文件名
     * @return 扩展名或空串
     */
    private String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        return dot < 0 ? "" : originalFilename.substring(dot + 1).toLowerCase();
    }

    /**
     * 解析知识库名称：缺省使用文件名主体，校验长度。
     *
     * @param name            请求传入名称
     * @param originalFilename 原始文件名
     * @return 知识库名称
     */
    private String resolveName(String name, String originalFilename) {
        String resolved = (name == null || name.isBlank())
                ? baseNameOf(originalFilename)
                : name.trim();
        if (resolved.length() > MAX_NAME_LENGTH) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "知识库名称长度超过上限");
        }
        return resolved.isBlank() ? "未命名知识库" : resolved;
    }

    /**
     * 提取文件名主体（去除目录与扩展名），用于缺省知识库名称。
     *
     * @param originalFilename 原始文件名
     * @return 文件名主体
     */
    private String baseNameOf(String originalFilename) {
        String base = sanitizeFileName(originalFilename);
        int dot = base.lastIndexOf('.');
        return dot > 0 ? base.substring(0, dot) : base;
    }

    /**
     * 规范化文件名：去除路径成分，仅保留基本文件名，防止路径穿越。
     *
     * @param originalFilename 原始文件名
     * @return 规范化后的文件名
     */
    private String sanitizeFileName(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        // 去除任意目录前缀（兼容 / 与 \ 分隔符）
        String base = originalFilename.replaceAll(".*[\\\\/]", "").trim();
        return base.length() > 255 ? base.substring(0, 255) : base;
    }

    /**
     * 实体转响应模型。
     *
     * @param kb 知识库实体
     * @return 知识库响应
     */
    private KnowledgeBaseResponse toResponse(KnowledgeBase kb) {
        return new KnowledgeBaseResponse(kb.getId(), kb.getName(), kb.getStatus(), kb.getFileName(),
                kb.getChunkCount() == null ? 0 : kb.getChunkCount(), kb.getVectorStore(),
                kb.getEmbeddingModel(), kb.getFailureReason(), kb.getCreatedAt());
    }
}
