package com.hieu.order_service.infrastructure.persistence.mapper;

import com.hieu.order_service.domain.model.order.Order;
import com.hieu.order_service.domain.model.order.OrderItem;
import com.hieu.order_service.domain.model.order.valueobject.*;
import com.hieu.order_service.infrastructure.persistence.jpa.entities.OrderItemJpaEntity;
import com.hieu.order_service.infrastructure.persistence.jpa.entities.OrderJpaEntity;
import org.springframework.stereotype.Component;

/** Maps between Order aggregate and OrderJpaEntity. syncGeneratedIds pattern mirrors catalog-service. */
@Component
public class OrderJpaMapper {

    public OrderJpaEntity toJpa(Order order, OrderJpaEntity existing) {
        var e = existing != null ? existing : new OrderJpaEntity();
        if (order.getId() != null) e.setId(order.getId().value());

        e.setOrderNumber(order.getOrderNumber().value());
        e.setUserId(order.getUserId().value());
        e.setStatus(order.getStatus().name());
        e.setSubtotalAmount(order.getSubtotalAmount().amount());
        e.setDiscountAmount(order.getDiscountAmount().amount());
        e.setShippingFee(order.getShippingFee().amount());
        e.setTotalAmount(order.getTotalAmount().amount());
        e.setVoucherCode(order.getVoucherCode());
        e.setRecipientName(order.getRecipientName().value());
        e.setRecipientPhone(order.getRecipientPhone().value());
        var addr = order.getShippingAddress();
        e.setStreet(addr.street());
        e.setWard(addr.ward());
        e.setDistrict(addr.district());
        e.setCity(addr.city());
        e.setCountry(addr.country());
        e.setPostalCode(addr.postalCode());
        e.setNotes(order.getNotes());
        e.setPaymentMethod(order.getPaymentMethod());
        e.setPaymentId(order.getPaymentId());
        e.setReservationId(order.getReservationId() == null ? null : order.getReservationId().value());
        e.setShipmentId(order.getShipmentId());
        e.setIdempotencyKey(order.getIdempotencyKey());
        e.setFailureReason(order.getFailureReason());
        e.setCreatedAt(order.getCreatedAt());
        e.setUpdatedAt(order.getUpdatedAt());
        e.setCompletedAt(order.getCompletedAt());
        e.setDeliveredAt(order.getDeliveredAt());
        e.setCancelledAt(order.getCancelledAt());
        e.setCreatedBy(order.getCreatedBy());
        e.setUpdatedBy(order.getUpdatedBy());
        if (order.getVersion() != null) e.setVersion(order.getVersion());

        // Sync items
        e.getItems().clear();
        order.getItems().forEach(item -> {
            var ie = new OrderItemJpaEntity();
            if (item.getId() != null) ie.setId(item.getId());
            ie.setOrder(e);
            ie.setProductId(item.getProductId().value());
            ie.setProductName(item.getProductName().value());
            ie.setVariantId(item.getVariantId());
            ie.setVariantSku(item.getVariantSku());
            ie.setVariantImage(item.getVariantImage());
            ie.setUnitPrice(item.getUnitPrice().amount());
            ie.setQuantity(item.getQuantity().value());
            e.getItems().add(ie);
        });

        return e;
    }

    public Order toDomain(OrderJpaEntity e) {
        var order = Order.reconstitute(
                OrderId.of(e.getId()),
                OrderNumber.of(e.getOrderNumber()),
                UserId.of(e.getUserId()),
                OrderStatus.valueOf(e.getStatus()),
                Money.of(e.getSubtotalAmount()),
                Money.of(e.getDiscountAmount()),
                Money.of(e.getShippingFee()),
                Money.of(e.getTotalAmount()),
                e.getVoucherCode(),
                RecipientName.of(e.getRecipientName()),
                RecipientPhone.of(e.getRecipientPhone()),
                ShippingAddress.of(e.getStreet(), e.getWard(), e.getDistrict(), e.getCity(), e.getCountry(), e.getPostalCode()),
                e.getNotes(),
                e.getPaymentMethod(),
                e.getPaymentId(),
                e.getReservationId() == null ? null : ReservationId.of(e.getReservationId()),
                e.getShipmentId(),
                e.getIdempotencyKey(),
                e.getFailureReason(),
                e.getCreatedAt(), e.getUpdatedAt(), e.getCompletedAt(),
                e.getDeliveredAt(), e.getCancelledAt(),
                e.getCreatedBy(), e.getUpdatedBy(), e.getVersion()
        );

        e.getItems().forEach(ie -> order.addReconstitutedItem(OrderItem.reconstitute(
                ie.getId(),
                ProductId.of(ie.getProductId()),
                ProductName.of(ie.getProductName()),
                ie.getVariantId(),
                ie.getVariantSku(),
                ie.getVariantImage(),
                Money.of(ie.getUnitPrice()),
                Quantity.of(ie.getQuantity())
        )));

        return order;
    }

    /** Syncs DB-generated ids back to the domain aggregate (mirrors catalog syncGeneratedIds). */
    public void syncGeneratedIds(Order order, OrderJpaEntity saved) {
        order.assignId(saved.getId());
        var savedItems = saved.getItems();
        var domainItems = order.getItems();
        for (int i = 0; i < Math.min(domainItems.size(), savedItems.size()); i++) {
            domainItems.get(i).assignId(savedItems.get(i).getId());
        }
    }
}
