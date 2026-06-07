package com.hieu.flash_sale_service.kafka;

import java.time.Instant;

/** Integration event emitted after a slot is successfully reserved. */
public record FlashSaleSlotReservedEvent(
        String eventId,
        Instant occurredOn,
        String saleId,
        String userId,
        int quantity,
        int remainingSlots
) {}
