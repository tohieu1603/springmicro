package com.hieu.shipping_service.kafka;

/** Payload received from {@code payment.completed} topic. */
public record PaymentCompletedEvent(
        String eventId,
        String orderId,
        String userId,
        String paymentStatus
) {}
