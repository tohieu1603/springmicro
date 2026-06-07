package com.hieu.auth_service.config;

import com.hieu.auth_service.application.events.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka bootstrap configuration.
 *
 * <p>Declares the service's outbound topics; Spring's {@code KafkaAdmin} picks the beans
 * up on startup and ensures they exist (idempotent — re-runs are safe).
 *
 * <p>Producer / consumer serialisation is configured via {@code spring.kafka.*} properties
 * in {@code application.yaml} (JSON serialisation). Auth-service only publishes at the
 * moment, but {@code @EnableKafka} is still turned on so future in-process listeners
 * (e.g. audit-log consumers) plug in without extra wiring.
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    /** Lifecycle events (registration, email/password changes, role mutations). */
    @Bean
    public NewTopic userEventsTopic() {
        return TopicBuilder.name(KafkaTopics.AUTH_USER_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /** Session + token lifecycle events (logins, rotations, revocations). */
    @Bean
    public NewTopic sessionEventsTopic() {
        return TopicBuilder.name(KafkaTopics.AUTH_SESSION_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
