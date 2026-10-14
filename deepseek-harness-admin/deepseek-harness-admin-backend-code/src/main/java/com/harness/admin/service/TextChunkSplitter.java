package com.harness.admin.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件说明：文本切分服务。
 *
 * <p>按行切分文档，将相邻行合并为不超过指定字符数的片段。
 * 超长单行按上限硬切，避免单个片段过大影响嵌入质量。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Component
public class TextChunkSplitter {

    /**
     * 将文档文本切分为片段。
     *
     * @param content   文档文本
     * @param maxChars  单片段最大字符数
     * @return 片段列表
     */
    public List<String> split(String content, int maxChars) {
        String normalized = content.replace("\r\n", "\n").trim();
        if (normalized.isEmpty()) {
            return List.of();
        }
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String block : normalized.split("\n")) {
            if (block.length() > maxChars) {
                // 先落盘当前片段，再硬切超长行
                flush(current, chunks);
                hardSplit(block, maxChars, chunks);
                continue;
            }
            if (current.length() > 0 && current.length() + 1 + block.length() > maxChars) {
                flush(current, chunks);
            }
            if (current.length() > 0) {
                current.append('\n');
            }
            current.append(block);
        }
        flush(current, chunks);
        return chunks;
    }

    /**
     * 将累积片段加入结果并清空缓冲。
     *
     * @param current 缓冲
     * @param chunks  结果
     */
    private void flush(StringBuilder current, List<String> chunks) {
        if (current.length() > 0) {
            chunks.add(current.toString());
            current.setLength(0);
        }
    }

    /**
     * 超长行按上限硬切。
     *
     * @param block    超长文本
     * @param maxChars 单片段上限
     * @param chunks   结果
     */
    private void hardSplit(String block, int maxChars, List<String> chunks) {
        for (int i = 0; i < block.length(); i += maxChars) {
            chunks.add(block.substring(i, Math.min(i + maxChars, block.length())));
        }
    }
}
