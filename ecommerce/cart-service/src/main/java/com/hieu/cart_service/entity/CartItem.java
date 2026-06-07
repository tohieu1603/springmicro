package com.hieu.cart_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a single cart line item (user × variant).
 * The UNIQUE constraint on (user_id, variant_id) drives upsert semantics.
 */
@Entity
@Table(
    name = "cart_items",
    uniqueConstraints = @UniqueConstraint(name = "uq_cart_user_variant", columnNames = {"user_id", "variant_id"}),
    indexes = @Index(name = "idx_cart_items_user_id", columnList = "user_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "variant_id", nullable = false, length = 36)
    private String variantId;

    @Column(name = "variant_sku", nullable = false, length = 64)
    private String variantSku;

    @Column(name = "variant_image", length = 1024)
    private String variantImage;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
