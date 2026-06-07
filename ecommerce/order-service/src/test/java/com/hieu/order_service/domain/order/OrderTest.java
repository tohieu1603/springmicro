package com.hieu.order_service.domain.order;

import com.hieu.order_service.domain.events.order.OrderCancelledEvent;
import com.hieu.order_service.domain.exception.InvalidOrderStateException;
import com.hieu.order_service.domain.model.order.Order;
import com.hieu.order_service.domain.model.order.OrderItem;
import com.hieu.order_service.domain.model.order.valueobject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Order aggregate")
class OrderTest {
    /**
     * Top-level smoke test — bắt buộc để Sonar rule java:S2187 nhận diện
     * class này có @Test (không tính các @Test bên trong @Nested).
     * Mọi test thật sự được tổ chức trong các @Nested class bên dưới.
     */
    @Test
    @DisplayName("class loads + JUnit discovers @Nested tests")
    void smokeTest_classDiscovered() {
    }

    // ── Fixture helpers ─────────────────────────────────────────────────────────

    record OrderFixture(
            UserId userId,
            OrderNumber number,
            RecipientName recipientName,
            RecipientPhone recipientPhone,
            ShippingAddress shippingAddress
    ) {}

    static final String USER_UUID   = "11111111-1111-1111-1111-111111111111";
    static final String PROD_ID_1   = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    static final String ORDER_ID_1  = "00000000-0000-0000-0000-000000000001";
    static final String SHIPMENT_1  = "00000000-0000-0000-0000-000000000011";
    static final String SHIPMENT_99 = "00000000-0000-0000-0000-000000000099";

    static OrderFixture fixture() {
        return new OrderFixture(
                UserId.of(USER_UUID),
                OrderNumber.of("ORD-20260101-000001"),
                RecipientName.of("Nguyen Van A"),
                RecipientPhone.of("0901234567"),
                new ShippingAddress("123 Le Loi", "Ben Thanh", "District 1", "Ho Chi Minh", "VN", "70000")
        );
    }

    static Order newOrder(String voucherCode) {
        var f = fixture();
        return Order.create(f.userId(), f.number(), f.recipientName(), f.recipientPhone(),
                f.shippingAddress(), "COD", null, voucherCode, "idem-1", USER_UUID);
    }

    static Order newOrder() { return newOrder(null); }

    static OrderItem item(long price, int qty) {
        return OrderItem.create(
                ProductId.of(PROD_ID_1), ProductName.of("Product A"),
                null, "SKU-001", null,
                Money.of(BigDecimal.valueOf(price)), Quantity.of(qty));
    }

    // Assign a fake DB id so domain methods that need it don't NPE
    static Order persistedOrder() {
        var o = newOrder();
        o.addItem(item(100_000, 2));
        o.assignId(ORDER_ID_1);
        return o;
    }

    // ── Nested test classes ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Lifecycle: trạng thái chuẩn")
    class Lifecycle {

        Order order;

        @BeforeEach
        void setup() { order = persistedOrder(); }

        @Test
        @DisplayName("create() → status=PENDING")
        void create_statusIsPending() {
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("markInventoryReserved() → INVENTORY_RESERVED")
        void markInventoryReserved_transitions() {
            order.markInventoryReserved(ReservationId.of("res-42"));
            assertThat(order.getStatus()).isEqualTo(OrderStatus.INVENTORY_RESERVED);
        }

        @Test
        @DisplayName("markPaymentPending() → PAYMENT_PENDING")
        void markPaymentPending_transitions() {
            order.markInventoryReserved(ReservationId.of("res-1"));
            order.markPaymentPending();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        }

        @Test
        @DisplayName("markPaymentCompleted() → PAYMENT_COMPLETED")
        void markPaymentCompleted_transitions() {
            order.markInventoryReserved(ReservationId.of("res-1"));
            order.markPaymentPending();
            order.markPaymentCompleted();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_COMPLETED);
        }

        @Test
        @DisplayName("confirm() → CONFIRMED, completedAt set")
        void confirm_transitions() {
            order.markInventoryReserved(ReservationId.of("res-1"));
            order.markPaymentPending();
            order.markPaymentCompleted();
            order.confirm();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(order.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("markShipped() → SHIPPED")
        void markShipped_transitions() {
            order.markInventoryReserved(ReservationId.of("res-1"));
            order.markPaymentPending();
            order.markPaymentCompleted();
            order.confirm();
            order.markShipped(SHIPMENT_99);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
            assertThat(order.getShipmentId()).isEqualTo(SHIPMENT_99);
        }

        @Test
        @DisplayName("markDelivered() → DELIVERED, deliveredAt set")
        void markDelivered_transitions() {
            order.markInventoryReserved(ReservationId.of("res-1"));
            order.markPaymentPending();
            order.markPaymentCompleted();
            order.confirm();
            order.markShipped(SHIPMENT_99);
            order.markDelivered();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
            assertThat(order.getDeliveredAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("StateTransitions: invalid transitions throw")
    class StateTransitions {

        @Test
        @DisplayName("cancel() sau DELIVERED → InvalidOrderStateException")
        void cancelAfterDelivered_throws() {
            var o = persistedOrder();
            o.markInventoryReserved(ReservationId.of("res-1"));
            o.markPaymentPending();
            o.markPaymentCompleted();
            o.confirm();
            o.markShipped(SHIPMENT_1);
            o.markDelivered();
            assertThatThrownBy(() -> o.cancel("too late"))
                    .isInstanceOf(InvalidOrderStateException.class);
        }

        @Test
        @DisplayName("markPaymentCompleted() hai lần liên tiếp → InvalidOrderStateException")
        void markPaymentCompleted_twice_throws() {
            var o = persistedOrder();
            o.markInventoryReserved(ReservationId.of("res-1"));
            o.markPaymentPending();
            o.markPaymentCompleted();
            assertThatThrownBy(o::markPaymentCompleted)
                    .isInstanceOf(InvalidOrderStateException.class);
        }

        @Test
        @DisplayName("confirm() từ PENDING (bỏ qua INVENTORY_RESERVED) → InvalidOrderStateException")
        void confirmFromPending_throws() {
            var o = persistedOrder();
            assertThatThrownBy(o::confirm)
                    .isInstanceOf(InvalidOrderStateException.class);
        }
    }

    @Nested
    @DisplayName("Discount: tính tổng tiền có giảm giá")
    class Discount {

        @Test
        @DisplayName("applyDiscount() → totalAmount = subtotal - discount + shipping")
        void applyDiscount_calculatesCorrectly() {
            var o = newOrder();
            o.addItem(item(500_000, 2));   // subtotal = 1,000,000
            o.applyShippingFee(Money.of(30_000L));
            o.applyDiscount(Money.of(100_000L));

            // total = 1,000,000 - 100,000 + 30,000 = 930,000
            assertThat(o.getTotalAmount().amount())
                    .isEqualByComparingTo(BigDecimal.valueOf(930_000));
        }

        @Test
        @DisplayName("subtotalAmount = sum of all item subtotals")
        void subtotal_isSumOfItems() {
            var o = newOrder();
            o.addItem(item(200_000, 3));  // 600,000
            o.addItem(item(50_000, 1));   // 50,000

            assertThat(o.getSubtotalAmount().amount())
                    .isEqualByComparingTo(BigDecimal.valueOf(650_000));
        }

        @Test
        @DisplayName("discount lớn hơn subtotal → totalAmount không âm (Money.subtract guards)")
        void discountExceedsSubtotal_totalIsZero() {
            var o = newOrder();
            o.addItem(item(100_000, 1));
            o.applyDiscount(Money.of(200_000L)); // more than subtotal

            assertThat(o.getTotalAmount().amount())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("VoucherCancel: OrderCancelledEvent giữ voucherCode")
    class VoucherCancel {

        @Test
        @DisplayName("cancel() trên order có voucher → event.voucherCode == code")
        void cancel_withVoucher_eventContainsVoucherCode() {
            var o = newOrder("SUMMER20");
            o.assignId(ORDER_ID_1);
            o.markInventoryReserved(ReservationId.of("res-1"));
            o.markPaymentPending();
            o.cancel("customer request");

            var events = o.peekDomainEvents();
            var cancelledEvent = events.stream()
                    .filter(e -> e instanceof OrderCancelledEvent)
                    .map(e -> (OrderCancelledEvent) e)
                    .findFirst();

            assertThat(cancelledEvent).isPresent();
            assertThat(cancelledEvent.get().voucherCode()).isEqualTo("SUMMER20");
        }

        @Test
        @DisplayName("cancel() trên order không có voucher → event.voucherCode == null")
        void cancel_withoutVoucher_eventVoucherCodeIsNull() {
            var o = newOrder(null);
            o.assignId(ORDER_ID_1);
            o.cancel("no voucher cancel");

            var events = o.peekDomainEvents();
            var cancelledEvent = events.stream()
                    .filter(e -> e instanceof OrderCancelledEvent)
                    .map(e -> (OrderCancelledEvent) e)
                    .findFirst();

            assertThat(cancelledEvent).isPresent();
            assertThat(cancelledEvent.get().voucherCode()).isNull();
        }
    }
}
