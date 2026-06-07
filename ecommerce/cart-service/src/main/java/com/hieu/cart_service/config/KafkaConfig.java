package com.hieu.cart_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka configuration for cart-service.
 * Cart only consumes — no topics owned, no NewTopic beans needed.
 * Consumer props are wired via spring.kafka.consumer.* in application.yaml.
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    // H1: Retry 3× with 1s backoff, then publish to DLT — mirrors order-service pattern.
    // Without this, uncaught exceptions silently commit the offset and lose events.
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> kafkaOps) {
        var recoverer = new DeadLetterPublishingRecoverer(kafkaOps,
                (rec, ex) -> new org.apache.kafka.common.TopicPartition(
                        rec.topic() + ".DLT", rec.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
    }

    @Bean
    public NewTopic cartConsumerDlt() {
        return TopicBuilder.name("cart-service.DLT").partitions(1).replicas(1).build();
    }
}
