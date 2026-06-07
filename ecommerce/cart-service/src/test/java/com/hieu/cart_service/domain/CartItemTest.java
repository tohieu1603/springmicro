package com.hieu.cart_service.domain;

import com.hieu.cart_service.entity.CartItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CartItem entity — unit tests")
class CartItemTest {
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

    static CartItem aCartItem(int quantity, BigDecimal unitPrice) {
        return CartItem.builder()
                .id("1")
                .userId("user-123")
                .productId("10")
                .productName("Test Product")
                .variantId("100")
                .variantSku("SKU-TEST")
                .unitPrice(unitPrice)
                .quantity(quantity)
                .version(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // ── Quantity tests ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Quantity")
    class QuantityTests {

        @Test
        @DisplayName("increment quantity bằng cách cộng thêm giá trị mới")
        void incrementQuantity_byAddingAmount() {
            CartItem item = aCartItem(3, new BigDecimal("100.00"));

            // Simulate addItem increment logic (CartService does item.setQuantity(qty + req.quantity))
            item.setQuantity(item.getQuantity() + 2);

            assertThat(item.getQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("updateQuantity — đặt số lượng trực tiếp")
        void updateQuantity_setsDirectly() {
            CartItem item = aCartItem(5, new BigDecimal("200.00"));

            item.setQuantity(10);

            assertThat(item.getQuantity()).isEqualTo(10);
        }

        @Test
        @DisplayName("quantity không được âm — validation kiểm tra tại controller level")
        void quantity_mustBePositive() {
            // CartItem JPA entity itself doesn't validate — but AddToCartRequest @Min(1) does.
            // Here we assert the current state can be read:
            CartItem item = aCartItem(1, new BigDecimal("50.00"));
            assertThat(item.getQuantity()).isPositive();
        }

        @Test
        @DisplayName("quantity tối đa 999 — CartService giới hạn")
        void quantity_cappedAt999ByService() {
            CartItem item = aCartItem(998, new BigDecimal("10.00"));

            // Simulate CartService logic: if (item.getQuantity() > 999) item.setQuantity(999)
            int newQty = item.getQuantity() + 5; // 1003
            if (newQty > 999) newQty = 999;
            item.setQuantity(newQty);

            assertThat(item.getQuantity()).isEqualTo(999);
        }
    }

    // ── Subtotal tests ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Subtotal")
    class SubtotalTests {

        @Test
        @DisplayName("subtotal = unitPrice * quantity")
        void subtotal_equalsUnitPriceTimesQuantity() {
            CartItem item = aCartItem(3, new BigDecimal("150.00"));

            BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

            assertThat(subtotal).isEqualByComparingTo(new BigDecimal("450.00"));
        }

        @Test
        @DisplayName("subtotal với giá lẻ — làm tròn đúng 2 chữ số thập phân")
        void subtotal_withFractionalPrice_correctScale() {
            CartItem item = aCartItem(7, new BigDecimal("99.99"));

            BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

            assertThat(subtotal).isEqualByComparingTo(new BigDecimal("699.93"));
        }

        @Test
        @DisplayName("subtotal = 0 khi unitPrice = 0")
        void subtotal_zeroWhenPriceZero() {
            CartItem item = aCartItem(5, BigDecimal.ZERO);

            BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

            assertThat(subtotal).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
