package com.hieu.notification_service.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka consumer configuration.
 * Untyped JSON deserialization: HashMap as default type, no type headers.
 * DefaultErrorHandler retries 3x then routes to .DLT topic.
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> ops) {
        var recoverer = new DeadLetterPublishingRecoverer(ops,
                (rec, ex) -> new TopicPartition(rec.topic() + ".DLT", rec.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
    }
}
