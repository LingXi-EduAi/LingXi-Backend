package com.lxe.lx.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AiTaskExecutionConfig {

    @Bean("aiTaskExecutor")
    public TaskExecutor aiTaskExecutor(
            @Value("${ai.task.executor.core-size:4}") int coreSize,
            @Value("${ai.task.executor.max-size:8}") int maxSize,
            @Value("${ai.task.executor.queue-capacity:100}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("ai-task-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }

    @Bean("aiModelCallLogExecutor")
    public TaskExecutor aiModelCallLogExecutor(
            @Value("${ai.model-log.executor.core-size:1}") int coreSize,
            @Value("${ai.model-log.executor.max-size:2}") int maxSize,
            @Value("${ai.model-log.executor.queue-capacity:200}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("ai-model-log-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}
