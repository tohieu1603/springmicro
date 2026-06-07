package com.hieu.order_service.infrastructure.messaging;

import com.hieu.order_service.application.common.DomainEventPublisher;
import com.hieu.order_service.domain.exception.OrderNotFoundException;
import com.hieu.order_service.domain.model.order.valueobject.OrderNumber;
import com.hieu.order_service.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/** Consumes shipping events and transitions order to DELIVERED. */
@Component
@RequiredArgsConstructor
public class ShippingEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ShippingEventConsumer.class);

    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;

    @KafkaListener(topics = {"shipping.status-changed", "shipping.delivered"}, groupId = "order-service")
    @Transactional
    public void onShippingEvent(Map<String, Object> payload,
                                org.apache.kafka.clients.consumer.ConsumerRecord<String, Object> record) {
        var topic = record.topic();
        var orderNumber = payload.get("orderId") != null ? payload.get("orderId").toString() : null;
        // Malformed payload — ack & drop, retry won't help.
        if (orderNumber == null) { log.warn("Shipping event missing orderId, dropping"); return; }

        var status = payload.get("status") != null ? payload.get("status").toString() : "";

        // Let exceptions propagate so Spring Kafka's DefaultErrorHandler retries → DLT
        // instead of silently committing the offset and losing the state transition.
        if ("shipping.delivered".equals(topic) || "DELIVERED".equalsIgnoreCase(status)) {
            var order = orderRepository.findByOrderNumber(OrderNumber.of(orderNumber))
                    .orElseThrow(() -> new OrderNotFoundException(orderNumber));
            if (order.getStatus().canTransitionTo(com.hieu.order_service.domain.model.order.valueobject.OrderStatus.DELIVERED)) {
                order.markDelivered();
                var saved = orderRepository.save(order);
                eventPublisher.publishEventsOf(saved);
            }
        }
    }
}
