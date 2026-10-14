package com.harness.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harness.admin.config.FileStorageService;
import com.harness.admin.domain.knowledgebase.KnowledgeBaseResponse;
import com.harness.admin.domain.knowledgebase.KnowledgeBaseStatus;
import com.harness.admin.domain.knowledgebase.KnowledgeBaseTaskStatus;
import com.harness.admin.mapper.KnowledgeBaseMapper;
import com.harness.admin.mapper.KnowledgeBaseTaskMapper;
import com.harness.admin.mapper.entity.KnowledgeBase;
import com.harness.admin.mapper.entity.KnowledgeBaseTask;
import com.harness.admin.service.KnowledgeBaseService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文件说明：知识库向量化全流程集成测试。
 *
 * <p>覆盖：上传 txt 后异步向量化最终 READY（片段数与模型标识记录）、
 * 不支持解析类型最终 FAILED 并记录原因、重处理保持 READY 且片段数不变、
 * 原始文件删除后重处理经有限重试后 FAILED（不误标 READY）。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@SpringBootTest
@AutoConfigureMockMvc
class VectorizationFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private KnowledgeBaseTaskMapper taskMapper;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 验证上传 txt 后异步向量化成功，知识库进入 READY 且记录片段数与模型标识。
     */
    @Test
    void uploadTxt_shouldBecomeReadyAfterVectorization() throws Exception {
        long kbId = upload("doc.txt", "text/plain", "第一段内容。\n第二段内容。", "说明文档");

        KnowledgeBaseResponse ready = awaitStatus(kbId, KnowledgeBaseStatus.READY);

        assertThat(ready.chunkCount()).isGreaterThan(0);
        assertThat(ready.embeddingModel()).isNotBlank();
        assertThat(ready.vectorStore()).isNotBlank();
        // 任务记录已成功
        Long successCount = taskMapper.selectCount(new LambdaQueryWrapper<KnowledgeBaseTask>()
                .eq(KnowledgeBaseTask::getKnowledgeBaseId, kbId)
                .eq(KnowledgeBaseTask::getStatus, KnowledgeBaseTaskStatus.SUCCESS));
        assertThat(successCount).isGreaterThanOrEqualTo(1);
    }

    /**
     * 验证不支持解析的类型经有限重试后进入 FAILED 并记录原因，绝不标记 READY。
     */
    @Test
    void uploadPdf_shouldFailWithReason() throws Exception {
        long kbId = upload("doc.pdf", "application/pdf", "pdf 内容", "PDF 文档");

        KnowledgeBaseResponse failed = awaitStatus(kbId, KnowledgeBaseStatus.FAILED);

        assertThat(failed.failureReason()).contains("暂不支持解析");
        // 失败知识库不允许被误标 READY
        assertThat(failed.status()).isEqualTo(KnowledgeBaseStatus.FAILED);
    }

    /**
     * 验证重处理成功后片段数稳定（不产生重复向量片段）。
     */
    @Test
    void reprocess_shouldKeepReadyWithSameChunkCount() throws Exception {
        long kbId = upload("doc.txt", "text/plain", "重处理内容", "知识库");
        KnowledgeBaseResponse before = awaitStatus(kbId, KnowledgeBaseStatus.READY);

        knowledgeBaseService.reprocess(kbId);
        KnowledgeBaseResponse after = awaitStatus(kbId, KnowledgeBaseStatus.READY);

        assertThat(after.chunkCount()).isEqualTo(before.chunkCount());
        assertThat(after.status()).isEqualTo(KnowledgeBaseStatus.READY);
    }

    /**
     * 验证原始文件删除后重处理：解析失败并经历有限重试，最终 FAILED 且不误标 READY。
     */
    @Test
    void reprocess_afterFileDeleted_shouldEventuallyFail() throws Exception {
        long kbId = upload("doc.txt", "text/plain", "内容", "知识库");
        awaitStatus(kbId, KnowledgeBaseStatus.READY);
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        fileStorageService.delete(kb.getStorageKey());

        knowledgeBaseService.reprocess(kbId);
        KnowledgeBaseResponse failed = awaitStatus(kbId, KnowledgeBaseStatus.FAILED);

        assertThat(failed.failureReason()).isNotBlank();
        assertThat(failed.status()).isEqualTo(KnowledgeBaseStatus.FAILED);
    }

    /**
     * 管理员上传知识库文件并返回知识库 ID。
     */
    private long upload(String fileName, String contentType, String content, String name) throws Exception {
        String token = loginToken();
        // 显式 UTF-8 编码，避免平台默认字符集（如 GBK）导致解析乱码
        MockMultipartFile file = new MockMultipartFile("file", fileName, contentType,
                content.getBytes(StandardCharsets.UTF_8));
        MvcResult result = mockMvc.perform(multipart("/api/knowledge-bases")
                        .file(file)
                        .param("name", name)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.data.id")).longValue();
    }

    /**
     * 轮询等待知识库状态，超时返回当前状态。
     */
    private KnowledgeBaseResponse awaitStatus(long kbId, KnowledgeBaseStatus expected) throws InterruptedException {
        KnowledgeBaseResponse current = knowledgeBaseService.detail(kbId);
        for (int i = 0; i < 60 && current.status() != expected; i++) {
            Thread.sleep(200);
            current = knowledgeBaseService.detail(kbId);
        }
        return current;
    }

    /**
     * 管理员登录获取凭证。
     */
    private String loginToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Admin@12345\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.<String>read(result.getResponse().getContentAsString(), "$.data.token");
    }
}
