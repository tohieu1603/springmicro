package com.hieu.order_service.domain.service;

import com.hieu.order_service.domain.model.order.Order;
import com.hieu.order_service.domain.model.order.OrderItem;
import com.hieu.order_service.domain.model.order.valueobject.*;
import org.springframework.stereotype.Service;

import java.util.List;

/** Pure domain service — stateless, no infra deps. */
@Service
public class OrderDomainService {

    public Order createOrder(UserId userId, OrderNumber orderNumber,
                             RecipientName recipientName, RecipientPhone recipientPhone,
                             ShippingAddress shippingAddress, String paymentMethod,
                             String notes, String voucherCode, String idempotencyKey,
                             List<OrderItem> items, String createdBy) {
        var order = Order.create(userId, orderNumber, recipientName, recipientPhone,
                shippingAddress, paymentMethod, notes, voucherCode, idempotencyKey, createdBy);
        items.forEach(order::addItem);
        return order;
    }
}
