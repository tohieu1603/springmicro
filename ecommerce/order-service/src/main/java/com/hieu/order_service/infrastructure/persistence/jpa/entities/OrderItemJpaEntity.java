package com.hieu.order_service.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter @Setter
public class OrderItemJpaEntity {

    @Id
    @Column(length = 36)
    private String id;

    @PrePersist
    void assignId() {
        if (id == null) id = java.util.UUID.randomUUID().toString();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderJpaEntity order;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "variant_id", length = 36)
    private String variantId;

    @Column(name = "variant_sku")
    private String variantSku;

    @Column(name = "variant_image")
    private String variantImage;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;
}
