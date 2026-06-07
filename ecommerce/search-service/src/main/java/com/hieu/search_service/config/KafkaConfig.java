package com.hieu.search_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka consumer config is driven by spring.kafka.consumer.* in application.yaml.
 * Untyped JSON deserialization: HashMap as default type, no type headers.
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    /**
     * Retry 3x with 1s delay, then publish to <original-topic>.DLT so poison
     * messages don't block the partition indefinitely.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> kafkaOps) {
        var recoverer = new DeadLetterPublishingRecoverer(kafkaOps,
                (rec, ex) -> new TopicPartition(rec.topic() + ".DLT", rec.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
    }

    /** Pre-create the DLT topic so the broker doesn't auto-create with default settings. */
    @Bean
    public NewTopic searchServiceDlt() {
        return new NewTopic("search-service.DLT", 1, (short) 1);
    }
}
