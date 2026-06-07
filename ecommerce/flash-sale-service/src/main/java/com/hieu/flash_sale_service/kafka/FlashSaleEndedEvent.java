package com.hieu.flash_sale_service.kafka;

import java.time.Instant;

/** Integration event emitted when a flash sale ends. */
public record FlashSaleEndedEvent(
        String eventId,
        Instant occurredOn,
        String saleId,
        String productId,
        int reservedSlots
) {}
