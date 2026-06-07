package com.hieu.catalog_service.config;

import com.hieu.catalog_service.application.events.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the outbound Kafka topics — {@code KafkaAdmin} creates them on startup
 * (idempotent). Producer / consumer serialisation is wired via {@code spring.kafka.*}
 * in {@code application.yaml}.
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    private static NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }

    @Bean public NewTopic productCreated()         { return topic(KafkaTopics.PRODUCT_CREATED); }
    @Bean public NewTopic productUpdated()         { return topic(KafkaTopics.PRODUCT_UPDATED); }
    @Bean public NewTopic productStatusChanged()   { return topic(KafkaTopics.PRODUCT_STATUS_CHANGED); }
    @Bean public NewTopic productDeleted()         { return topic(KafkaTopics.PRODUCT_DELETED); }
    @Bean public NewTopic variantAdded()           { return topic(KafkaTopics.VARIANT_ADDED); }
    @Bean public NewTopic variantRemoved()         { return topic(KafkaTopics.VARIANT_REMOVED); }
    @Bean public NewTopic variantStockChanged()    { return topic(KafkaTopics.VARIANT_STOCK_CHANGED); }
    @Bean public NewTopic variantPriceChanged()    { return topic(KafkaTopics.VARIANT_PRICE_CHANGED); }
    @Bean public NewTopic categoryCreated()        { return topic(KafkaTopics.CATEGORY_CREATED); }
    @Bean public NewTopic categoryUpdated()        { return topic(KafkaTopics.CATEGORY_UPDATED); }
    @Bean public NewTopic categoryDeleted()        { return topic(KafkaTopics.CATEGORY_DELETED); }
    @Bean public NewTopic attrCreated()            { return topic(KafkaTopics.ATTR_CREATED); }
    @Bean public NewTopic attrUpdated()            { return topic(KafkaTopics.ATTR_UPDATED); }
    @Bean public NewTopic attrDeleted()            { return topic(KafkaTopics.ATTR_DELETED); }
}
