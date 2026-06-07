package com.hieu.inventory_service.kafka;

import com.hieu.inventory_service.entity.InventoryEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes {@code inventory.low-stock} Kafka events after transaction commits.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LowStockEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ApplicationEventPublisher eventPublisher;

    private static final String TOPIC = "inventory.low-stock";

    /** Emits an application event (picked up by {@link #onLowStock(LowStockEvent)}) if stock is low. */
    public void publishIfLowStock(InventoryEntity inv) {
        if (inv.isLowStock()) {
            eventPublisher.publishEvent(new LowStockEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                inv.getProductId(),
                inv.getSku(),
                inv.getAvailableQuantity(),
                inv.getMinStockLevel()
            ));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLowStock(LowStockEvent event) {
        log.warn("[LOW-STOCK] productId={} sku={} available={} threshold={}",
            event.productId(), event.sku(), event.quantity(), event.minStockLevel());
        kafkaTemplate.send(TOPIC, String.valueOf(event.productId()), Map.of(
            "eventId", event.eventId(),
            "occurredOn", event.occurredOn().toString(),
            "productId", event.productId(),
            "sku", event.sku(),
            "quantity", event.quantity(),
            "minStockLevel", event.minStockLevel()
        ));
    }

    /** Internal application event record. */
    public record LowStockEvent(
        String eventId,
        Instant occurredOn,
        String productId,
        String sku,
        int quantity,
        int minStockLevel
    ) {}
}
