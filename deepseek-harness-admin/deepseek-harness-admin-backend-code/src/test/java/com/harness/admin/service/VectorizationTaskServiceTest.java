package com.harness.admin.service;

import com.harness.admin.config.StorageProperties;
import com.harness.admin.config.VectorizationProperties;
import com.harness.admin.config.FileStorageService;
import com.harness.admin.domain.common.BizException;
import com.harness.admin.domain.common.ErrorCode;
import com.harness.admin.domain.knowledgebase.KnowledgeBaseTaskStatus;
import com.harness.admin.mapper.KnowledgeBaseMapper;
import com.harness.admin.mapper.KnowledgeBaseTaskMapper;
import com.harness.admin.mapper.entity.KnowledgeBaseTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 文件说明：向量化任务治理服务单元测试。
 *
 * <p>覆盖幂等守卫：活跃任务拒绝重复创建、并发重复插入兜底、任务创建与幂等键生成。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@ExtendWith(MockitoExtension.class)
class VectorizationTaskServiceTest {

    @Mock
    private KnowledgeBaseTaskMapper taskMapper;

    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private TransactionTemplate transactionTemplate;

    private VectorizationTaskService service;

    /** 同步执行器：任务立即在测试线程执行 */
    private final TaskExecutor syncExecutor = Runnable::run;

    @BeforeEach
    void setUp() {
        VectorizationProperties props = new VectorizationProperties();
        props.setMaxRetries(2);
        VectorIndexService index = new VectorIndexService(org.mockito.Mockito.mock(VectorStore.class));
        FileStorageService storage = new FileStorageService(new StorageProperties());
        VectorizationTaskStateService stateService =
                new VectorizationTaskStateService(taskMapper, knowledgeBaseMapper, transactionTemplate, props);
        service = new VectorizationTaskService(taskMapper, knowledgeBaseMapper, new DocumentParser(),
                new TextChunkSplitter(), index, storage, props, syncExecutor, taskScheduler, stateService);
    }

    @Test
    void startFor_withActiveTask_shouldReject() {
        when(taskMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.startFor(1L, "file-key.txt"))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.KNOWLEDGE_BASE_PROCESSING);
    }

    @Test
    void startFor_duplicateInsert_shouldReturnQuietly() {
        when(taskMapper.selectCount(any())).thenReturn(0L);
        when(taskMapper.insert(any(KnowledgeBaseTask.class)))
                .thenThrow(new DuplicateKeyException("unique constraint violated"));

        // 并发竞态下唯一索引兜底，不重复提交也不抛错
        service.startFor(1L, "file-key.txt");
    }

    @Test
    void startFor_shouldCreatePendingTaskWithIdempotencyKey() {
        when(taskMapper.selectCount(any())).thenReturn(0L);
        when(taskMapper.insert(any(KnowledgeBaseTask.class))).thenReturn(1);

        service.startFor(1L, "file-key.txt");

        ArgumentCaptor<KnowledgeBaseTask> captor = ArgumentCaptor.forClass(KnowledgeBaseTask.class);
        verify(taskMapper).insert(captor.capture());
        KnowledgeBaseTask task = captor.getValue();
        assertThat(task.getStatus()).isEqualTo(KnowledgeBaseTaskStatus.PENDING);
        assertThat(task.getFileVersion()).isEqualTo("file-key.txt");
        assertThat(task.getIdempotencyKey()).isNotBlank();
        assertThat(task.getRetryCount()).isZero();
    }

    @Test
    void startFor_withoutKnowledgeBase_shouldExitSafely() {
        // 同步执行器立即执行：任务对象不存在时直接返回，不产生异常
        when(taskMapper.selectCount(any())).thenReturn(0L);
        when(taskMapper.insert(any(KnowledgeBaseTask.class))).thenReturn(1);
        when(taskMapper.selectById(any())).thenReturn(null);

        service.startFor(1L, "file-key.txt");
    }
}
