package com.hieu.flash_sale_service.kafka;

import java.time.Instant;

/** Integration event emitted when a flash sale is activated (started). */
public record FlashSaleStartedEvent(
        String eventId,
        Instant occurredOn,
        String saleId,
        String productId,
        Instant startTime,
        Instant endTime,
        int totalSlots
) {}
