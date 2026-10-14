package com.harness.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文件说明：文件存储与上传校验配置。
 *
 * <p>知识库原始文件存储目录、允许的文件扩展名与大小上限均在此配置；
 * 存储目录通过环境变量注入，便于部署时指向独立磁盘。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Data
@Component
@ConfigurationProperties(prefix = "harness.storage")
public class StorageProperties {

    /** 知识库原始文件存储目录 */
    private String knowledgeBaseDir = "./data/knowledge-bases";

    /** 允许上传的文件扩展名（不包含点，小写） */
    private List<String> allowedExtensions = List.of("txt", "md", "pdf", "docx");

    /** 上传文件大小上限（MB） */
    private long maxFileSizeMb = 20;
}
