package com.hieu.notification_service.kafka;

/** Central list of Kafka topic names consumed by this service. */
public final class KafkaTopics {

    private KafkaTopics() {}

    public static final String ORDER_PLACED     = "order.placed";
    public static final String ORDER_CONFIRMED  = "order.confirmed";
    public static final String ORDER_CANCELLED  = "order.cancelled";
    public static final String ORDER_SHIPPED    = "order.shipped";
    public static final String ORDER_DELIVERED  = "order.delivered";

    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED    = "payment.failed";

    public static final String SHIPPING_STATUS_CHANGED = "shipping.status-changed";
}
