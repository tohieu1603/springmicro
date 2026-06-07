package com.hieu.shipping_service.kafka;

/** Kafka topic name constants for shipping-service. */
public final class ShippingTopics {

    public static final String PAYMENT_COMPLETED    = "payment.completed";
    public static final String SHIPPING_STATUS_CHANGED = "shipping.status-changed";
    public static final String SHIPPING_DELIVERED   = "shipping.delivered";

    private ShippingTopics() {}
}
