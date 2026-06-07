package com.hieu.inventory_service.dto;

import lombok.*;

import java.time.Instant;

/** Read model returned for inventory queries. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryDTO {
    private String id;
    private String productId;
    private String sku;
    private Integer quantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private Integer minStockLevel;
    private boolean lowStock;
    private Instant lastUpdated;
    private Long version;
}
