package com.hieu.payment_service.messaging;

/** Kafka topic names — single source of truth for payment-service. */
public final class KafkaTopics {

    private KafkaTopics() {}

    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED    = "payment.failed";
    public static final String PAYMENT_REFUNDED  = "payment.refunded";
}
