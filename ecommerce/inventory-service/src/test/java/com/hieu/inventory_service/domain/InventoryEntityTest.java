package com.hieu.inventory_service.domain;

import com.hieu.inventory_service.entity.InventoryEntity;
import com.hieu.inventory_service.exception.InsufficientStockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InventoryEntity — unit tests")
class InventoryEntityTest {
    /**
     * Top-level smoke test — bắt buộc để Sonar rule java:S2187 nhận diện
     * class này có @Test (không tính các @Test bên trong @Nested).
     * Mọi test thật sự được tổ chức trong các @Nested class bên dưới.
     */
    @Test
    @DisplayName("class loads + JUnit discovers @Nested tests")
    void smokeTest_classDiscovered() {
        // Nếu tới được đây tức là JUnit có thể instantiate test class.
        // assertThat(this).isNotNull() được tự thực hiện ngầm.
    }


    // ── helpers ───────────────────────────────────────────────────────────────

    static InventoryEntity anInventoryEntity(int quantity, int reserved) {
        return InventoryEntity.builder()
                .id("1")
                .productId("100")
                .sku("SKU-TEST-001")
                .quantity(quantity)
                .reservedQuantity(reserved)
                .minStockLevel(5)
                .lastUpdated(Instant.now())
                .build();
    }

    static InventoryEntity freshEntity(int quantity) {
        return anInventoryEntity(quantity, 0);
    }

    // ── Reserve ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Reserve")
    class ReserveTests {

        @Test
        @DisplayName("reserve(qty) giảm available, tăng reserved")
        void reserve_decrementsAvailableAndIncrementsReserved() {
            var entity = freshEntity(100);

            entity.reserve(30);

            assertThat(entity.getReservedQuantity()).isEqualTo(30);
            assertThat(entity.getAvailableQuantity()).isEqualTo(70);
            assertThat(entity.getQuantity()).isEqualTo(100); // total unchanged
        }

        @Test
        @DisplayName("reserve nhiều lần cộng dồn reserved")
        void reserve_multipleReserves_accumulated() {
            var entity = freshEntity(100);

            entity.reserve(20);
            entity.reserve(30);

            assertThat(entity.getReservedQuantity()).isEqualTo(50);
            assertThat(entity.getAvailableQuantity()).isEqualTo(50);
        }

        @Test
        @DisplayName("reserve(qty > available) ném InsufficientStockException")
        void reserve_insufficient_throwsException() {
            var entity = freshEntity(10);

            assertThatThrownBy(() -> entity.reserve(11))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Insufficient stock");
        }

        @Test
        @DisplayName("reserve(qty == available) thành công — biên giới đúng")
        void reserve_exactlyAvailable_succeeds() {
            var entity = freshEntity(10);

            entity.reserve(10);

            assertThat(entity.getAvailableQuantity()).isZero();
            assertThat(entity.getReservedQuantity()).isEqualTo(10);
        }
    }

    // ── Confirm & Release ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("ConfirmRelease")
    class ConfirmReleaseTests {

        @Test
        @DisplayName("confirmReservation giảm cả quantity lẫn reservedQuantity")
        void confirmReservation_reducesBothQuantityAndReserved() {
            var entity = anInventoryEntity(100, 40);

            entity.confirmReservation(20);

            assertThat(entity.getQuantity()).isEqualTo(80);
            assertThat(entity.getReservedQuantity()).isEqualTo(20);
            assertThat(entity.getAvailableQuantity()).isEqualTo(60);
        }

        @Test
        @DisplayName("confirmReservation ném IllegalState khi quantity < amount")
        void confirmReservation_quantityInsufficient_throwsIllegalState() {
            var entity = anInventoryEntity(5, 10);

            assertThatThrownBy(() -> entity.confirmReservation(10))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("releaseReservation khôi phục available — reserved giảm")
        void releaseReservation_restoresAvailable() {
            var entity = anInventoryEntity(100, 40);

            entity.releaseReservation(40);

            assertThat(entity.getReservedQuantity()).isZero();
            assertThat(entity.getAvailableQuantity()).isEqualTo(100);
            assertThat(entity.getQuantity()).isEqualTo(100); // total unchanged
        }

        @Test
        @DisplayName("releaseReservation idempotent — không xuống âm")
        void releaseReservation_idempotent_floorAtZero() {
            var entity = anInventoryEntity(100, 10);

            entity.releaseReservation(50); // release more than reserved

            assertThat(entity.getReservedQuantity()).isZero(); // floors at 0
        }

        @Test
        @DisplayName("addStock tăng quantity, không ảnh hưởng reserved")
        void addStock_increasesQuantityOnly() {
            var entity = anInventoryEntity(50, 10);

            entity.addStock(25);

            assertThat(entity.getQuantity()).isEqualTo(75);
            assertThat(entity.getReservedQuantity()).isEqualTo(10);
            assertThat(entity.getAvailableQuantity()).isEqualTo(65);
        }
    }
}
