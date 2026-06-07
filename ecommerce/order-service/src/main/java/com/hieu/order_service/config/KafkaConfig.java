package com.hieu.order_service.config;

import com.hieu.order_service.application.events.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/** Declares outbound Kafka topics. KafkaAdmin creates them idempotently on startup. */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Dedicated string-to-string KafkaTemplate for the outbox poller.
     * Payload is already serialised JSON; no JsonSerializer needed.
     */
    @Bean
    public KafkaTemplate<String, String> stringKafkaTemplate() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    private static NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }

    @Bean public NewTopic orderPlaced()           { return topic(KafkaTopics.ORDER_PLACED); }
    @Bean public NewTopic orderConfirmed()        { return topic(KafkaTopics.ORDER_CONFIRMED); }
    @Bean public NewTopic orderShipped()          { return topic(KafkaTopics.ORDER_SHIPPED); }
    @Bean public NewTopic orderDelivered()        { return topic(KafkaTopics.ORDER_DELIVERED); }
    @Bean public NewTopic orderCancelled()        { return topic(KafkaTopics.ORDER_CANCELLED); }
    @Bean public NewTopic orderFailed()           { return topic(KafkaTopics.ORDER_FAILED); }
    @Bean public NewTopic orderReturnRequested()  { return topic(KafkaTopics.ORDER_RETURN_REQUESTED); }
    @Bean public NewTopic orderReturnApproved()   { return topic(KafkaTopics.ORDER_RETURN_APPROVED); }
    @Bean public NewTopic orderReturnRejected()   { return topic(KafkaTopics.ORDER_RETURN_REJECTED); }
    @Bean public NewTopic orderReturned()         { return topic(KafkaTopics.ORDER_RETURNED); }

    // Outbox dead-letter topic — poller parks events here after exceeding max retries.
    @Bean public NewTopic orderOutboxDlt() {
        return TopicBuilder.name(com.hieu.order_service.infrastructure.outbox.OutboxPoller.DLT_TOPIC)
                .partitions(1).replicas(1).build();
    }

    // Inbound consumers (PaymentEventConsumer / ShippingEventConsumer) re-throw on
    // failure; this handler retries 3× with 1s backoff then publishes to <topic>.DLT.
    // Without it, exceptions silently commit the offset and lose events.
    @Bean public NewTopic consumerDlt()           { return TopicBuilder.name("order-service.DLT").partitions(1).replicas(1).build(); }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaOps) {
        var recoverer = new DeadLetterPublishingRecoverer(kafkaOps,
                (rec, ex) -> new org.apache.kafka.common.TopicPartition(rec.topic() + ".DLT", rec.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
    }
}
