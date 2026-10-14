package com.harness.admin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件说明：知识库原始文件存储服务。
 *
 * <p>将上传文件按随机存储键写入可配置目录，返回存储键供数据库保存；
 * 管理接口不向客户端暴露真实文件路径。删除由存储键定位文件。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Component
public class FileStorageService {

    /** 结构化日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    /** 存储配置 */
    private final StorageProperties storageProperties;

    /**
     * 构造文件存储服务。
     *
     * @param storageProperties 存储配置
     */
    public FileStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    /**
     * 保存上传文件到存储目录。
     *
     * @param file       上传文件
     * @param storageKey 存储键（随机生成）
     */
    public void save(MultipartFile file, String storageKey) {
        Path target = resolve(storageKey);
        try {
            Files.createDirectories(target.getParent());
            // 原子替换写入，避免半写文件；文件名使用随机存储键，不信任客户端文件名
            file.transferTo(target);
            log.info("knowledge base file stored, storageKey={}", storageKey);
        } catch (IOException ex) {
            throw new IllegalStateException("知识库文件保存失败", ex);
        }
    }

    /**
     * 按存储键删除文件。
     *
     * @param storageKey 存储键
     */
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException ex) {
            log.warn("delete knowledge base file failed, storageKey={}", storageKey, ex);
        }
    }

    /**
     * 根据存储键解析文件路径（供解析器读取原始文件）。
     *
     * @param storageKey 存储键
     * @return 文件路径
     */
    public Path resolve(String storageKey) {
        Path dir = Path.of(storageProperties.getKnowledgeBaseDir()).toAbsolutePath().normalize();
        return dir.resolve(storageKey);
    }
}
