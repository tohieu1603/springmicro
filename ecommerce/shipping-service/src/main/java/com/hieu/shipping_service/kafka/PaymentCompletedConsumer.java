package com.hieu.shipping_service.kafka;

import com.hieu.shipping_service.dto.CreateShipmentRequest;
import com.hieu.shipping_service.service.OrderServiceClient;
import com.hieu.shipping_service.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes {@code payment.completed} events and auto-creates a PENDING shipment
 * by fetching shipping address from order-service via HTTP.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedConsumer {

    private final ShipmentService shipmentService;
    private final OrderServiceClient orderServiceClient;

    @KafkaListener(topics = ShippingTopics.PAYMENT_COMPLETED, groupId = "shipping-service")
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received payment.completed: orderId={} userId={}", event.orderId(), event.userId());

        var orderOpt = orderServiceClient.fetchOrder(event.orderId());
        if (orderOpt.isEmpty()) {
            log.warn("Cannot create shipment — order-service unavailable for orderId={}", event.orderId());
            return;
        }

        var order = orderOpt.get();
        var shippingAddress = extractMap(order, "shippingAddress");
        if (shippingAddress == null || shippingAddress.isEmpty()) {
            log.warn("Order {} has no shippingAddress, skipping auto-create shipment", event.orderId());
            return;
        }

        // Let exceptions propagate so DefaultErrorHandler retries + routes to .DLT.
        // Previously catch-all silently dropped failed shipments — order paid but
        // never fulfilled. createShipmentIfAbsent handles the legitimate "already
        // exists" case via DataIntegrityViolationException internally.
        var req = new CreateShipmentRequest(
                event.orderId(),
                event.userId(),
                null,
                str(shippingAddress, "recipientName"),
                str(shippingAddress, "recipientPhone"),
                str(shippingAddress, "addressLine"),
                str(shippingAddress, "ward"),
                str(shippingAddress, "district"),
                strOrDefault(shippingAddress, "city", "Unknown"),
                strOrDefault(shippingAddress, "country", "Vietnam"),
                null
        );
        shipmentService.createShipmentIfAbsent(req);
        log.info("Auto-created shipment for orderId={}", event.orderId());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractMap(Map<String, Object> map, String key) {
        var val = map.get(key);
        if (val instanceof Map<?, ?> m) return (Map<String, Object>) m;
        return null;
    }

    private static String str(Map<String, Object> map, String key) {
        var v = map.get(key);
        return v != null ? v.toString() : null;
    }

    private static String strOrDefault(Map<String, Object> map, String key, String def) {
        var v = map.get(key);
        return (v != null && !v.toString().isBlank()) ? v.toString() : def;
    }
}
