package com.harness.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 文件说明：任务调度配置。
 *
 * <p>提供向量化重试退避调度使用的 {@link TaskScheduler} Bean，
 * 独立线程池避免阻塞请求线程。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Configuration
public class TaskConfig {

    /**
     * 创建任务调度器（用于重试退避调度）。
     *
     * @return 任务调度器
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("vectorization-retry-");
        return scheduler;
    }
}
