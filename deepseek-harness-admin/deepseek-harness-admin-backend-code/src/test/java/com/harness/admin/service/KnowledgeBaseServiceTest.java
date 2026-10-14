package com.harness.admin.service;

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
import com.harness.admin.domain.user.UserRole;
import com.harness.admin.mapper.KnowledgeBaseMapper;
import com.harness.admin.mapper.entity.KnowledgeBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 文件说明：知识库服务单元测试。
 *
 * <p>覆盖上传校验（空文件/类型/大小/名称）、成功上传（状态与审计字段）、
 * 元数据入库失败清理文件、分页列表与详情/未找到路径。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeBaseServiceTest {

    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private VectorizationTaskService vectorizationTaskService;

    private StorageProperties storageProperties;

    private KnowledgeBaseService service;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        service = new KnowledgeBaseService(knowledgeBaseMapper, fileStorageService, storageProperties,
                vectorizationTaskService);
        AuthContext.set(new UserPrincipal(1L, "admin", UserRole.ADMIN));
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void upload_shouldCreateUploadingRecordWithAudit() {
        when(knowledgeBaseMapper.insert(any(KnowledgeBase.class))).thenReturn(1);
        MockMultipartFile file = new MockMultipartFile("file", "手册.txt", "text/plain", "内容".getBytes());

        KnowledgeBaseResponse response = service.upload(file, "产品手册");

        assertThat(response.status()).isEqualTo(KnowledgeBaseStatus.UPLOADING);
        assertThat(response.name()).isEqualTo("产品手册");
        assertThat(response.fileName()).isEqualTo("手册.txt");
        assertThat(response.chunkCount()).isZero();
        ArgumentCaptor<KnowledgeBase> captor = ArgumentCaptor.forClass(KnowledgeBase.class);
        verify(knowledgeBaseMapper).insert(captor.capture());
        // 审计字段取自认证上下文，存储键随机生成
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(1L);
        assertThat(captor.getValue().getStorageKey()).endsWith(".txt");
        verify(fileStorageService).save(eq(file), any());
    }

    @Test
    void upload_emptyFile_shouldThrowValidationFailed() {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> service.upload(file, "知识库"))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void upload_unsupportedExtension_shouldThrowValidationFailed() {
        MockMultipartFile file = new MockMultipartFile("file", "virus.exe", "application/octet-stream", "x".getBytes());

        assertThatThrownBy(() -> service.upload(file, "知识库"))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
        verify(fileStorageService, never()).save(any(), any());
    }

    @Test
    void upload_oversizedFile_shouldThrowValidationFailed() {
        storageProperties.setMaxFileSizeMb(1);
        MockMultipartFile file = new MockMultipartFile("file", "big.pdf", "application/pdf", new byte[2 * 1024 * 1024]);

        assertThatThrownBy(() -> service.upload(file, "知识库"))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void upload_withoutName_shouldDeriveFromFileName() {
        when(knowledgeBaseMapper.insert(any(KnowledgeBase.class))).thenReturn(1);
        MockMultipartFile file = new MockMultipartFile("file", "指南.md", "text/markdown", "内容".getBytes());

        KnowledgeBaseResponse response = service.upload(file, null);

        assertThat(response.name()).isEqualTo("指南");
    }

    @Test
    void upload_insertFailure_shouldCleanupStoredFile() {
        when(knowledgeBaseMapper.insert(any(KnowledgeBase.class))).thenThrow(new RuntimeException("db down"));
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "内容".getBytes());

        assertThatThrownBy(() -> service.upload(file, "知识库"))
                .isInstanceOf(RuntimeException.class);
        // 元数据入库失败时删除已落盘文件，避免残留
        verify(fileStorageService).delete(any());
    }

    @Test
    void list_shouldReturnPagedResponses() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(1L);
        kb.setName("知识库A");
        kb.setStatus(KnowledgeBaseStatus.READY);
        kb.setFileName("a.txt");
        kb.setChunkCount(10);
        Page<KnowledgeBase> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(kb));
        when(knowledgeBaseMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<KnowledgeBaseResponse> result = service.list(1, 10);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).name()).isEqualTo("知识库A");
        assertThat(result.items().get(0).status()).isEqualTo(KnowledgeBaseStatus.READY);
    }

    @Test
    void detail_found_shouldReturnResponse() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(7L);
        kb.setName("知识库B");
        when(knowledgeBaseMapper.selectById(7L)).thenReturn(kb);

        KnowledgeBaseResponse response = service.detail(7L);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.name()).isEqualTo("知识库B");
    }

    @Test
    void detail_notFound_shouldThrowResourceNotFound() {
        when(knowledgeBaseMapper.selectById(9L)).thenReturn(null);

        assertThatThrownBy(() -> service.detail(9L))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
}
