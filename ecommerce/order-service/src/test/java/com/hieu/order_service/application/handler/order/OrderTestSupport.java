package com.hieu.order_service.application.handler.order;

import com.hieu.order_service.domain.model.order.Order;
import com.hieu.order_service.domain.model.order.OrderItem;
import com.hieu.order_service.domain.model.order.valueobject.*;

import java.math.BigDecimal;

/** Shared fixture builders for order handler unit tests. */
final class OrderTestSupport {

    private OrderTestSupport() {}

    /** A minimal PENDING order with one item and id=1 assigned. */
    static Order order(String userId) {
        var o = Order.create(
                UserId.of(userId),
                OrderNumber.of("ORD-20260101-000001"),
                RecipientName.of("Nguyen Van A"),
                RecipientPhone.of("0901234567"),
                new ShippingAddress("123 Le Loi", "Ben Thanh", "District 1", "Ho Chi Minh", "VN", "70000"),
                "COD", "note", null, "idem-1", userId);
        o.addItem(OrderItem.create(
                ProductId.of("11111111-1111-1111-1111-111111111111"), ProductName.of("Product A"),
                "10000000-0000-0000-0000-000000000001", "SKU-001", null,
                Money.of(BigDecimal.valueOf(100_000)), Quantity.of(2)));
        o.assignId("00000000-0000-0000-0000-000000000001");
        return o;
    }
}
