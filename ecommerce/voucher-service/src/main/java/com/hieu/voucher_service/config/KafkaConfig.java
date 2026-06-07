package com.hieu.voucher_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Kafka consumer config driven by spring.kafka.consumer.* in application.yaml.
 * Untyped JSON deserialization: HashMap as default type, no type headers.
 */
@Configuration
@EnableKafka
public class KafkaConfig {
}
