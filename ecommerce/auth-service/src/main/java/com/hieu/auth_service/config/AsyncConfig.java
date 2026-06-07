package com.hieu.auth_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * M1: Route @Async tasks through virtual threads — zero pool-sizing needed, low overhead.
     * spring.threads.virtual.enabled=true handles Tomcat; this covers @Async dispatch.
     */
    @Bean
    public TaskExecutor taskExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(task -> task); // keep MDC etc. passthrough
        // Delegate to a virtual-thread-per-task executor via the adapter.
        var delegate = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("auth-async-", 0).factory());
        return new org.springframework.scheduling.concurrent.ConcurrentTaskExecutor(delegate);
    }
}
