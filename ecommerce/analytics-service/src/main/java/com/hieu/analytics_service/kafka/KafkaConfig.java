package com.hieu.analytics_service.kafka;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

/** Untyped JSON deserialization driven by application.yaml — HashMap default type. */
@Configuration
@EnableKafka
public class KafkaConfig {
}
