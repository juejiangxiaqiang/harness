package com.harness.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harness.admin.config.FileStorageService;
import com.harness.admin.config.VectorizationProperties;
import com.harness.admin.domain.common.BizException;
import com.harness.admin.domain.common.ErrorCode;
import com.harness.admin.domain.knowledgebase.KnowledgeBaseTaskStatus;
import com.harness.admin.mapper.KnowledgeBaseMapper;
import com.harness.admin.mapper.KnowledgeBaseTaskMapper;
import com.harness.admin.mapper.entity.KnowledgeBase;
import com.harness.admin.mapper.entity.KnowledgeBaseTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件说明：知识库向量化任务治理服务。
 *
 * <p>负责任务创建（幂等键防并发重复）、异步执行（解析-&gt;切分-&gt;写入向量）、
 * 有限次数指数退避重试、失败原因记录与知识库状态流转。
 * 向量写入不完整时绝不允许将知识库标记为 READY。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Service
public class VectorizationTaskService {

    /** 结构化日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(VectorizationTaskService.class);

    /** 失败原因最大长度 */
    private static final int REASON_MAX_LENGTH = 500;

    /** 任务级互斥锁，防止同一任务并发执行 */
    private final ConcurrentHashMap<Long, Object> taskLocks = new ConcurrentHashMap<>();

    /** 任务表数据访问 */
    private final KnowledgeBaseTaskMapper taskMapper;

    /** 知识库表数据访问 */
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    /** 文档解析 */
    private final DocumentParser documentParser;

    /** 文本切分 */
    private final TextChunkSplitter chunkSplitter;

    /** 向量索引 */
    private final VectorIndexService vectorIndexService;

    /** 文件存储 */
    private final FileStorageService fileStorageService;

    /** 向量化配置 */
    private final VectorizationProperties vectorizationProperties;

    /** 异步执行器 */
    private final TaskExecutor taskExecutor;

    /** 重试调度器 */
    private final TaskScheduler taskScheduler;

    /** 任务与知识库状态持久化 */
    private final VectorizationTaskStateService stateService;

    /**
     * 构造向量化任务治理服务。
     *
     * @param taskMapper            任务表数据访问
     * @param knowledgeBaseMapper   知识库表数据访问
     * @param documentParser        文档解析
     * @param chunkSplitter         文本切分
     * @param vectorIndexService    向量索引
     * @param fileStorageService    文件存储
     * @param vectorizationProperties 向量化配置
     * @param taskExecutor          异步执行器
     * @param taskScheduler         重试调度器
     * @param stateService          状态持久化服务
     */
    public VectorizationTaskService(KnowledgeBaseTaskMapper taskMapper, KnowledgeBaseMapper knowledgeBaseMapper,
                                    DocumentParser documentParser, TextChunkSplitter chunkSplitter,
                                    VectorIndexService vectorIndexService, FileStorageService fileStorageService,
                                    VectorizationProperties vectorizationProperties, TaskExecutor taskExecutor,
                                    TaskScheduler taskScheduler, VectorizationTaskStateService stateService) {
        this.taskMapper = taskMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.documentParser = documentParser;
        this.chunkSplitter = chunkSplitter;
        this.vectorIndexService = vectorIndexService;
        this.fileStorageService = fileStorageService;
        this.vectorizationProperties = vectorizationProperties;
        this.taskExecutor = taskExecutor;
        this.taskScheduler = taskScheduler;
        this.stateService = stateService;
    }

    /**
     * 为知识库创建向量化任务并异步执行。
     *
     * @param kbId       知识库 ID
     * @param storageKey 文件存储键（作为文件版本标识）
     */
    public void startFor(Long kbId, String storageKey) {
        // 活跃任务存在时拒绝重复创建，防止并发重复向量化
        Long activeCount = taskMapper.selectCount(new LambdaQueryWrapper<KnowledgeBaseTask>()
                .eq(KnowledgeBaseTask::getKnowledgeBaseId, kbId)
                .in(KnowledgeBaseTask::getStatus,
                        KnowledgeBaseTaskStatus.PENDING, KnowledgeBaseTaskStatus.RUNNING));
        if (activeCount > 0) {
            throw new BizException(ErrorCode.KNOWLEDGE_BASE_PROCESSING, "知识库正在向量化处理");
        }
        // 幂等键 = 知识库 + 文件版本 + 任务序号，允许同文件多次重处理但拦截并发重复
        int seq = Math.toIntExact(taskMapper.selectCount(new LambdaQueryWrapper<KnowledgeBaseTask>()
                .eq(KnowledgeBaseTask::getKnowledgeBaseId, kbId)));
        KnowledgeBaseTask task = new KnowledgeBaseTask();
        task.setKnowledgeBaseId(kbId);
        task.setFileVersion(storageKey);
        task.setIdempotencyKey(sha256(kbId + ":" + storageKey + ":" + seq));
        task.setStatus(KnowledgeBaseTaskStatus.PENDING);
        task.setRetryCount(0);
        try {
            taskMapper.insert(task);
        } catch (DuplicateKeyException ex) {
            // 并发竞态下唯一索引兜底，视为已提交
            log.warn("duplicate vectorization task rejected, kbId={}", kbId);
            return;
        }
        taskExecutor.execute(() -> run(task.getId()));
    }

    /**
     * 执行任务主体：状态流转 + 解析切分 + 向量写入，失败按策略重试。
     *
     * @param taskId 任务 ID
     */
    private void run(Long taskId) {
        KnowledgeBaseTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        // 任务级锁防止重试调度与手动触发并发执行
        Object lock = taskLocks.computeIfAbsent(taskId, key -> new Object());
        synchronized (lock) {
            try {
                if (task.getStatus() == KnowledgeBaseTaskStatus.RUNNING
                        || task.getStatus() == KnowledgeBaseTaskStatus.SUCCESS) {
                    return;
                }
                stateService.markRunning(task);
                attempt(task);
            } finally {
                taskLocks.remove(taskId);
            }
        }
    }

    /**
     * 单次向量化尝试：解析、切分、写入向量，成功标记 READY。
     *
     * @param task 任务
     */
    private void attempt(KnowledgeBaseTask task) {
        try {
            KnowledgeBase kb = knowledgeBaseMapper.selectById(task.getKnowledgeBaseId());
            if (kb == null) {
                throw new IllegalStateException("知识库不存在");
            }
            if (task.getRetryCount() == 0) {
                // 首次执行（含重处理）前清理旧向量，避免旧片段残留
                vectorIndexService.deleteByKnowledgeBase(kb.getId());
            }
            String content = documentParser.parse(fileStorageService.resolve(kb.getStorageKey()),
                    extensionOf(kb.getStorageKey()));
            List<String> chunks = chunkSplitter.split(content, vectorizationProperties.getChunkMaxChars());
            int chunkCount = vectorIndexService.write(kb.getId(), kb.getStorageKey(), chunks);
            stateService.markSuccess(task, kb, chunkCount);
        } catch (RuntimeException ex) {
            handleFailure(task, ex);
        }
    }

    /**
     * 失败处理：未超上限则退避重试，超限则最终失败并记录原因。
     *
     * @param task 任务
     * @param ex   异常
     */
    private void handleFailure(KnowledgeBaseTask task, RuntimeException ex) {
        int retries = task.getRetryCount() + 1;
        String reason = shortenReason(ex.getMessage());
        if (retries <= vectorizationProperties.getMaxRetries()) {
            // 指数退避调度下一次尝试
            long delaySeconds = vectorizationProperties.getBackoffBaseSeconds() * (1L << (retries - 1));
            task.setRetryCount(retries);
            task.setStatus(KnowledgeBaseTaskStatus.PENDING);
            task.setFailureReason(reason);
            taskMapper.updateById(task);
            taskScheduler.schedule(() -> run(task.getId()), Instant.now().plusSeconds(delaySeconds));
            log.warn("vectorization retry scheduled, taskId={}, retryCount={}, reason={}",
                    task.getId(), retries, reason);
        } else {
            stateService.markFailed(task, reason);
        }
    }

    /**
     * 从存储键解析扩展名。
     *
     * @param storageKey 存储键
     * @return 扩展名
     */
    private String extensionOf(String storageKey) {
        int dot = storageKey.lastIndexOf('.');
        return dot < 0 ? "" : storageKey.substring(dot + 1).toLowerCase();
    }

    /**
     * 截断失败原因到安全长度。
     *
     * @param message 原始信息
     * @return 截断后的原因
     */
    private String shortenReason(String message) {
        if (message == null || message.isBlank()) {
            return "未知错误";
        }
        return message.length() > REASON_MAX_LENGTH ? message.substring(0, REASON_MAX_LENGTH) : message;
    }

    /**
     * 计算 SHA-256 十六进制摘要。
     *
     * @param value 原始值
     * @return 摘要
     */
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            // JDK 17 必然支持 SHA-256，此处仅防御性兜底
            throw new IllegalStateException("SHA-256 算法不可用", ex);
        }
    }
}
