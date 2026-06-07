package com.hieu.order_service.application.events;

/** Kafka topic names for order integration events. Single source of truth. */
public final class KafkaTopics {

    private KafkaTopics() {}

    public static final String ORDER_PLACED            = "order.placed";
    public static final String ORDER_CONFIRMED         = "order.confirmed";
    public static final String ORDER_SHIPPED           = "order.shipped";
    public static final String ORDER_DELIVERED         = "order.delivered";
    public static final String ORDER_CANCELLED         = "order.cancelled";
    public static final String ORDER_FAILED            = "order.failed";
    public static final String ORDER_RETURN_REQUESTED  = "order.return-requested";
    public static final String ORDER_RETURN_APPROVED   = "order.return-approved";
    public static final String ORDER_RETURN_REJECTED   = "order.return-rejected";
    public static final String ORDER_RETURNED          = "order.returned";
}
