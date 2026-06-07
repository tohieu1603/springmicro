package com.hieu.flash_sale_service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.TimeUnit;

/**
 * Listens to in-process domain events and publishes them to Kafka after TX commit.
 * Each send blocks on the ack for a bounded time so broker errors surface as logs
 * instead of being silently dropped by fire-and-forget.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlashSaleEventPublisher {

    private static final long SEND_TIMEOUT_SECONDS = 5;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStarted(FlashSaleStartedEvent event) {
        publish(KafkaTopics.FLASH_SALE_STARTED, String.valueOf(event.saleId()), event, "flashsale.started");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEnded(FlashSaleEndedEvent event) {
        publish(KafkaTopics.FLASH_SALE_ENDED, String.valueOf(event.saleId()), event, "flashsale.ended");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSlotReserved(FlashSaleSlotReservedEvent event) {
        publish(KafkaTopics.FLASH_SALE_SLOT_RESERVED, String.valueOf(event.saleId()), event, "flashsale.slot-reserved");
    }

    private void publish(String topic, String key, Object event, String label) {
        try {
            kafkaTemplate.send(topic, key, event).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.debug("Published {} key={}", label, key);
        } catch (Exception e) {
            // Tx already committed — broker outage means this event is lost. Log loudly
            // so ops can replay from DB state. Upgrade path: write to outbox table inside
            // the same tx instead of @TransactionalEventListener.
            log.error("Kafka publish FAILED for {} key={}: {}", label, key, e.getMessage(), e);
            if (Thread.currentThread().isInterrupted()) Thread.currentThread().interrupt();
        }
    }
}
