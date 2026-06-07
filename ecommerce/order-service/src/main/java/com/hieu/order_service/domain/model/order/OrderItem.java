package com.hieu.order_service.domain.model.order;

import com.hieu.order_service.domain.model.order.valueobject.Money;
import com.hieu.order_service.domain.model.order.valueobject.ProductId;
import com.hieu.order_service.domain.model.order.valueobject.ProductName;
import com.hieu.order_service.domain.model.order.valueobject.Quantity;
import lombok.Getter;

/** Line item within an Order. Subtotal is derived from unitPrice * quantity. */
@Getter
public class OrderItem {

    private String id;
    private final ProductId productId;
    private final ProductName productName;
    private final String variantId;
    private final String variantSku;
    private final String variantImage;
    private final Money unitPrice;
    private final Quantity quantity;

    private OrderItem(String id, ProductId productId, ProductName productName,
                      String variantId, String variantSku, String variantImage,
                      Money unitPrice, Quantity quantity) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.variantId = variantId;
        this.variantSku = variantSku;
        this.variantImage = variantImage;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public static OrderItem create(ProductId productId, ProductName productName,
                                   String variantId, String variantSku, String variantImage,
                                   Money unitPrice, Quantity quantity) {
        return new OrderItem(null, productId, productName, variantId, variantSku, variantImage, unitPrice, quantity);
    }

    public static OrderItem reconstitute(String id, ProductId productId, ProductName productName,
                                         String variantId, String variantSku, String variantImage,
                                         Money unitPrice, Quantity quantity) {
        return new OrderItem(id, productId, productName, variantId, variantSku, variantImage, unitPrice, quantity);
    }

    public Money subtotal() { return unitPrice.multiply(quantity.value()); }

    public void assignId(String id) { this.id = id; }
}
