package com.hieu.payment_service.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Integration event records published to Kafka. */
public final class PaymentIntegrationEvents {

    private PaymentIntegrationEvents() {}

    public record PaymentCompletedEvent(
            String eventId,
            Instant occurredOn,
            String paymentId,
            String orderId,
            String userId,
            BigDecimal amount,
            String currency,
            String method,
            String transactionId
    ) {
        public static PaymentCompletedEvent of(String paymentId, String orderId, String userId,
                                               BigDecimal amount, String currency,
                                               String method, String transactionId) {
            return new PaymentCompletedEvent(
                    UUID.randomUUID().toString(), Instant.now(),
                    paymentId, orderId, userId, amount, currency, method, transactionId);
        }
    }

    public record PaymentRefundedEvent(
            String eventId,
            Instant occurredOn,
            String paymentId,
            String orderId,
            String userId,
            BigDecimal refundAmount,
            String currency
    ) {
        public static PaymentRefundedEvent of(String paymentId, String orderId, String userId,
                                              BigDecimal refundAmount, String currency) {
            return new PaymentRefundedEvent(
                    UUID.randomUUID().toString(), Instant.now(),
                    paymentId, orderId, userId, refundAmount, currency);
        }
    }
}
