package com.hieu.order_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Saga + event executors backed by Java 25 virtual threads.
 *
 * <p>Saga fan-out (parallel catalog gRPC, payment REST, voucher REST) is I/O-bound;
 * virtual threads cost ~1KB each so there's no need to size pools. Blocking on a
 * virtual thread parks the carrier rather than the OS thread, eliminating the
 * core/max tuning that platform pools required.
 *
 * <p>{@code spring.threads.virtual.enabled=true} also flips Tomcat + @Async to
 * virtual threads — these named beans are kept so existing {@code @Qualifier}
 * injections continue to work.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "sagaExecutor")
    public Executor sagaExecutor() {
        return new TaskExecutorAdapter(
                Executors.newThreadPerTaskExecutor(
                        Thread.ofVirtual().name("order-saga-", 0).factory()));
    }

    @Bean(name = "eventExecutor")
    public Executor eventExecutor() {
        return new TaskExecutorAdapter(
                Executors.newThreadPerTaskExecutor(
                        Thread.ofVirtual().name("order-event-", 0).factory()));
    }
}
