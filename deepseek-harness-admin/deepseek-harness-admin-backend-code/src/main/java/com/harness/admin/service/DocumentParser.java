package com.harness.admin.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 文件说明：知识库文档解析服务。
 *
 * <p>按文件类型读取原始文件内容。当前支持纯文本与 Markdown；
 * 其他已允许上传但暂不支持解析的类型直接抛出异常，由任务治理记录失败原因。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Component
public class DocumentParser {

    /** 支持解析的类型 */
    private static final List<String> SUPPORTED = List.of("txt", "md");

    /**
     * 解析文档为纯文本。
     *
     * @param file      原始文件
     * @param extension 文件扩展名（小写）
     * @return 文档文本
     */
    public String parse(Path file, String extension) {
        if (!SUPPORTED.contains(extension)) {
            // 非法类型进入失败路径而非中断服务
            throw new IllegalStateException("该文件类型暂不支持解析: " + extension);
        }
        try {
            return Files.readString(file, java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("文档读取失败: " + ex.getMessage(), ex);
        }
    }
}
