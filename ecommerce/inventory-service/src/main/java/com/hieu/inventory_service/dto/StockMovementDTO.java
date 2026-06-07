package com.hieu.inventory_service.dto;

import com.hieu.inventory_service.entity.StockMovement;

import java.time.Instant;

/** Wire-friendly projection of {@link StockMovement}. */
public record StockMovementDTO(
        String id,
        String productId,
        String sku,
        int delta,
        int quantityBefore,
        int quantityAfter,
        int reservedAfter,
        String reason,
        String referenceId,
        String actor,
        String note,
        Instant createdAt
) {
    public static StockMovementDTO from(StockMovement m) {
        return new StockMovementDTO(
                m.getId(), m.getProductId(), m.getSku(),
                m.getDelta(), m.getQuantityBefore(), m.getQuantityAfter(),
                m.getReservedAfter(),
                m.getReason() == null ? null : m.getReason().name(),
                m.getReferenceId(), m.getActor(), m.getNote(), m.getCreatedAt());
    }
}
