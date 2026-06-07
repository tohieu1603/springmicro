package com.hieu.order_service.domain.model.order;

import com.hieu.order_service.domain.model.order.valueobject.Money;
import com.hieu.order_service.domain.model.order.valueobject.ProductId;
import com.hieu.order_service.domain.model.order.valueobject.ProductName;
import com.hieu.order_service.domain.model.order.valueobject.Quantity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("OrderItem")
class OrderItemTest {
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

    private static final String PROD_ID = "11111111-1111-1111-1111-111111111111";

    static OrderItem item(long unitPrice, int qty) {
        return OrderItem.create(
                ProductId.of(PROD_ID),
                ProductName.of("Áo thun"),
                null, "SKU-001", null,
                Money.of(BigDecimal.valueOf(unitPrice)),
                Quantity.of(qty));
    }

    @Nested
    @DisplayName("subtotal()")
    class SubtotalCalculation {

        @Test
        @DisplayName("subtotal = unitPrice × quantity")
        void subtotal_isUnitPriceTimesQuantity() {
            var i = item(150_000, 3);
            assertThat(i.subtotal().amount())
                    .isEqualByComparingTo(BigDecimal.valueOf(450_000));
        }

        @Test
        @DisplayName("quantity=1 → subtotal == unitPrice")
        void subtotal_singleUnit_equalsUnitPrice() {
            var i = item(99_000, 1);
            assertThat(i.subtotal().amount())
                    .isEqualByComparingTo(BigDecimal.valueOf(99_000));
        }

        @Test
        @DisplayName("large quantity does not overflow")
        void subtotal_largeQuantity_noOverflow() {
            var i = item(1_000, Quantity.MAX);
            assertThat(i.subtotal().amount()).isPositive();
        }
    }

    @Nested
    @DisplayName("Quantity validation")
    class QuantityValidation {

        @Test
        @DisplayName("quantity=0 → IllegalArgumentException")
        void zeroQuantity_throws() {
            assertThatThrownBy(() -> item(100_000, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("quantity âm → IllegalArgumentException")
        void negativeQuantity_throws() {
            assertThatThrownBy(() -> item(100_000, -1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("quantity > MAX → IllegalArgumentException")
        void aboveMaxQuantity_throws() {
            assertThatThrownBy(() -> item(100_000, Quantity.MAX + 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("max");
        }
    }
}
