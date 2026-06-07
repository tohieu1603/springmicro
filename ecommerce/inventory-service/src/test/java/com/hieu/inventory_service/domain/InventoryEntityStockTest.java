package com.hieu.inventory_service.domain;

import com.hieu.inventory_service.entity.InventoryEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link InventoryEntity} stock methods not exercised by
 * {@link InventoryEntityTest}: subtractStock guards, isLowStock and canReserve.
 */
@DisplayName("InventoryEntity stock guards (unit)")
class InventoryEntityStockTest {

    private static InventoryEntity inventory(int quantity, int reserved, int minStock) {
        return InventoryEntity.builder()
                .id("1").productId("100").sku("SKU-1")
                .quantity(quantity).reservedQuantity(reserved).minStockLevel(minStock)
                .build();
    }

    @Test
    @DisplayName("subtractStock reduces total quantity when above reserved")
    void subtractStock_ok() {
        var e = inventory(50, 10, 5);
        e.subtractStock(30);
        assertThat(e.getQuantity()).isEqualTo(20);
        assertThat(e.getReservedQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("subtractStock that would drop below reserved throws IllegalState")
    void subtractStock_belowReserved() {
        var e = inventory(50, 45, 5);
        assertThatThrownBy(() -> e.subtractStock(10)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("subtractStock rejects non-positive amounts")
    void subtractStock_nonPositive() {
        assertThatThrownBy(() -> inventory(50, 0, 5).subtractStock(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("confirmReservation rejects confirming more than reserved")
    void confirm_moreThanReserved() {
        var e = inventory(100, 5, 5);
        assertThatThrownBy(() -> e.confirmReservation(10)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("isLowStock is true when available drops to/below the minimum")
    void isLowStock() {
        assertThat(inventory(12, 0, 5).isLowStock()).isFalse();  // available 12 > 5
        assertThat(inventory(4, 0, 5).isLowStock()).isTrue();    // available 4 <= 5
        assertThat(inventory(20, 18, 5).isLowStock()).isTrue();  // available 2 <= 5
    }

    @Test
    @DisplayName("canReserve reflects available quantity")
    void canReserve() {
        var e = inventory(20, 5, 5); // available 15
        assertThat(e.canReserve(15)).isTrue();
        assertThat(e.canReserve(16)).isFalse();
    }
}
