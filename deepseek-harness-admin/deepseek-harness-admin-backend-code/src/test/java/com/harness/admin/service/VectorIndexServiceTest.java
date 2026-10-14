package com.harness.admin.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * 文件说明：向量索引服务单元测试。
 *
 * <p>覆盖稳定片段 ID、元数据写入与按知识库删除向量（重处理清理）。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@ExtendWith(MockitoExtension.class)
class VectorIndexServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Test
    void write_shouldUseStableChunkIdsAndMetadata() {
        VectorIndexService service = new VectorIndexService(vectorStore);
        List<String> chunks = List.of("片段A", "片段B");

        int count = service.write(3L, "file-key.txt", chunks);

        assertThat(count).isEqualTo(2);
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        List<Document> documents = captor.getValue();
        // 片段 ID 由知识库、文件版本与序号确定，保证重试写入幂等
        assertThat(documents.get(0).getId()).isEqualTo("kb-3-file-key.txt-0");
        assertThat(documents.get(1).getId()).isEqualTo("kb-3-file-key.txt-1");
        Map<String, Object> metadata = documents.get(0).getMetadata();
        assertThat(metadata.get(VectorIndexService.METADATA_KB_ID)).isEqualTo(3L);
        assertThat(metadata.get(VectorIndexService.METADATA_FILE_VERSION)).isEqualTo("file-key.txt");
    }

    @Test
    void deleteByKnowledgeBase_shouldDeleteWithKbFilter() {
        VectorIndexService service = new VectorIndexService(vectorStore);

        service.deleteByKnowledgeBase(7L);

        verify(vectorStore).delete(any(Filter.Expression.class));
    }
}
