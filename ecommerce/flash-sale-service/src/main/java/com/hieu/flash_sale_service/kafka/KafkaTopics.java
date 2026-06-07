package com.hieu.flash_sale_service.kafka;

/** Kafka topic name constants. */
public final class KafkaTopics {

    public static final String FLASH_SALE_STARTED       = "flashsale.started";
    public static final String FLASH_SALE_ENDED         = "flashsale.ended";
    public static final String FLASH_SALE_SLOT_RESERVED = "flashsale.slot-reserved";

    private KafkaTopics() {}
}
