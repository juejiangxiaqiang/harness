package com.harness.admin.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 文件说明：文本切分服务单元测试。
 *
 * <p>覆盖片段长度约束、超长行硬切、空内容与换行符归一化。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
class TextChunkSplitterTest {

    private final TextChunkSplitter splitter = new TextChunkSplitter();

    @Test
    void split_shortContent_shouldReturnSingleChunk() {
        List<String> chunks = splitter.split("第一行\n第二行", 100);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).contains("第一行", "第二行");
    }

    @Test
    void split_longContent_shouldSplitByLimit() {
        List<String> chunks = splitter.split("a".repeat(50) + "\n" + "b".repeat(50), 60);

        // 第一片段装下 50 个 a，第二行 b 与前缀超限后另起片段
        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0)).hasSize(50);
    }

    @Test
    void split_oversizeLine_shouldHardSplit() {
        List<String> chunks = splitter.split("x".repeat(200), 80);

        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0)).hasSize(80);
        assertThat(chunks.get(2)).hasSize(40);
    }

    @Test
    void split_emptyContent_shouldReturnEmptyList() {
        assertThat(splitter.split("", 100)).isEmpty();
        assertThat(splitter.split("  \n  ", 100)).isEmpty();
    }

    @Test
    void split_crlf_shouldNormalize() {
        List<String> chunks = splitter.split("第一行\r\n第二行", 100);

        assertThat(chunks.get(0)).isEqualTo("第一行\n第二行");
    }
}
