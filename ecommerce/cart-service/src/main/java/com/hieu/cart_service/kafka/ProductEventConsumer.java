package com.hieu.cart_service.kafka;

import com.hieu.cart_service.service.CartService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka consumer for catalog-service events.
 *
 * <p>Listens on {@code catalog.product-deleted} and {@code catalog.product-status-changed}.
 * When a product is deleted or becomes INACTIVE, purges all cart items for that product
 * and invalidates the Redis cache of affected users.
 */
@Component
@RequiredArgsConstructor
public class ProductEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProductEventConsumer.class);

    private final CartService cartService;

    /**
     * Handles product-deleted events. Expects payload with {@code productId} field.
     * H1: No blanket catch — exceptions propagate so DefaultErrorHandler retries then DLTs.
     */
    @KafkaListener(topics = "catalog.product-deleted", groupId = "${spring.kafka.consumer.group-id}")
    public void onProductDeleted(Map<String, Object> event) {
        var productId = extractProductId(event);
        if (productId == null) {
            log.warn("product-deleted event missing productId: {}", event);
            return;
        }
        log.info("Handling product-deleted: productId={}", productId);
        cartService.removeItemsByProduct(productId);
    }

    /**
     * Handles product-status-changed events.
     * Removes cart items when status transitions to INACTIVE or DELETED.
     * H1: No blanket catch — exceptions propagate so DefaultErrorHandler retries then DLTs.
     */
    @KafkaListener(topics = "catalog.product-status-changed", groupId = "${spring.kafka.consumer.group-id}")
    public void onProductStatusChanged(Map<String, Object> event) {
        var productId = extractProductId(event);
        var status = event.get("status");
        if (productId == null || status == null) {
            log.warn("product-status-changed event missing fields: {}", event);
            return;
        }
        var statusStr = status.toString().toUpperCase();
        if ("INACTIVE".equals(statusStr) || "DELETED".equals(statusStr)) {
            log.info("Handling product-status-changed: productId={} status={}", productId, statusStr);
            cartService.removeItemsByProduct(productId);
        }
    }

    private String extractProductId(Map<String, Object> event) {
        var raw = event.get("productId");
        if (raw == null) return null;
        return raw.toString();
    }
}
