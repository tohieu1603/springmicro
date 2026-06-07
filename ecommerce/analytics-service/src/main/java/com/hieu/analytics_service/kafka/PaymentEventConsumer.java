package com.hieu.analytics_service.kafka;

import com.hieu.analytics_service.service.AnalyticsIndexer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final AnalyticsIndexer indexer;

    @KafkaListener(topics = {
            "payment.completed", "payment.failed", "payment.refunded"
    }, groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentEvent(Map<String, Object> payload,
                               @Header(name = "kafka_receivedTopic", required = false) String topic,
                               Acknowledgment ack) {
        String fallbackType = topic != null ? topic.toUpperCase().replace('.', '_') : "PAYMENT_EVENT";
        // Exception propagates to Kafka error handler for retry/DLT; no ack on failure
        indexer.index(EventPayloadMapper.toDocument(payload, fallbackType, "PAYMENT"));
        ack.acknowledge();
    }
}
