package com.hieu.inventory_service.dto;

import com.hieu.inventory_service.entity.StockMovement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for the hand-written {@link StockMovementDTO#from(StockMovement)} mapper:
 * every entity field is copied 1:1 and the {@code Reason} enum is rendered as its name with
 * a null-safe guard.
 */
@DisplayName("StockMovementDTO.from (unit)")
class StockMovementDTOTest {

    @Test
    @DisplayName("copies every field and renders Reason as its enum name")
    void mapsAllFields() {
        Instant now = Instant.parse("2026-06-04T10:15:30Z");
        StockMovement m = StockMovement.builder()
                .id("11")
                .productId("100")
                .sku("SKU-100")
                .delta(-5)
                .quantityBefore(50)
                .quantityAfter(45)
                .reservedAfter(3)
                .reason(StockMovement.Reason.RESERVE)
                .referenceId("ORDER-9")
                .actor("admin")
                .note("manual reserve")
                .createdAt(now)
                .build();

        StockMovementDTO dto = StockMovementDTO.from(m);

        assertThat(dto.id()).isEqualTo("11");
        assertThat(dto.productId()).isEqualTo("100");
        assertThat(dto.sku()).isEqualTo("SKU-100");
        assertThat(dto.delta()).isEqualTo(-5);
        assertThat(dto.quantityBefore()).isEqualTo(50);
        assertThat(dto.quantityAfter()).isEqualTo(45);
        assertThat(dto.reservedAfter()).isEqualTo(3);
        assertThat(dto.reason()).isEqualTo("RESERVE");
        assertThat(dto.referenceId()).isEqualTo("ORDER-9");
        assertThat(dto.actor()).isEqualTo("admin");
        assertThat(dto.note()).isEqualTo("manual reserve");
        assertThat(dto.createdAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("null Reason maps to a null reason string (no NPE)")
    void nullReasonMapsToNull() {
        StockMovement m = StockMovement.builder()
                .id("1").productId("1").sku("S").delta(0)
                .quantityBefore(0).quantityAfter(0).reservedAfter(0)
                .reason(null)
                .build();

        StockMovementDTO dto = StockMovementDTO.from(m);

        assertThat(dto.reason()).isNull();
    }

    @Test
    @DisplayName("each Reason enum value is rendered verbatim")
    void everyReasonRendered() {
        for (StockMovement.Reason r : StockMovement.Reason.values()) {
            StockMovement m = StockMovement.builder()
                    .id("1").productId("1").sku("S").delta(0)
                    .quantityBefore(0).quantityAfter(0).reservedAfter(0)
                    .reason(r).build();
            assertThat(StockMovementDTO.from(m).reason()).isEqualTo(r.name());
        }
    }
}
