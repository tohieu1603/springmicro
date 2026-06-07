package com.hieu.catalog_service.domain.model.product;

import com.hieu.catalog_service.domain.model.product.valueobject.Money;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductId;
import com.hieu.catalog_service.domain.model.product.valueobject.Quantity;
import com.hieu.catalog_service.domain.model.product.valueobject.Sku;
import com.hieu.catalog_service.domain.model.product.valueobject.VariantId;
import com.hieu.catalog_service.domain.model.product.valueobject.VariantStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Variant — a sellable SKU row belonging to a {@link Product}.
 *
 * <p>Enclosed by the {@code Product} aggregate; callers reach it through
 * {@code Product#addVariant}, {@code Product#updateVariantStock}, etc. Business rules:
 * <ul>
 *   <li>Sale price, when set, must not exceed regular price.</li>
 *   <li>Stock is a non-negative {@link Quantity}; hitting zero flips status to
 *       {@link VariantStatus#OUT_OF_STOCK} unless already {@link VariantStatus#INACTIVE}.</li>
 *   <li>{@link VariantStatus#INACTIVE} is a merchandising decision — stock changes do not
 *       promote it back to {@link VariantStatus#ACTIVE}.</li>
 * </ul>
 */
@Getter
public final class Variant {

    private VariantId id;            // null until persisted
    private ProductId productId;     // null for variants of a not-yet-persisted product
    private Sku sku;
    private Money price;
    private Money cost;
    private Money salePrice;
    private String image;
    private BigDecimal weight;
    private Quantity quantity;
    private VariantStatus status;
    private Long version;

    private final List<VariantAttr> attrs = new ArrayList<>();

    private Variant(VariantId id, ProductId productId, Sku sku, Money price, Money cost, Money salePrice,
                    String image, BigDecimal weight, Quantity quantity, VariantStatus status, Long version) {
        this.id = id;
        this.productId = productId;
        this.sku = Objects.requireNonNull(sku, "sku");
        this.price = Objects.requireNonNull(price, "price");
        this.cost = cost;
        this.salePrice = salePrice;
        this.image = image;
        this.weight = weight;
        this.quantity = Objects.requireNonNull(quantity, "quantity");
        this.status = Objects.requireNonNull(status, "status");
        this.version = version;
        validateSalePrice();
    }

    // ── Factories ────────────────────────────────────────────────────────────

    public static Variant create(ProductId productId, Sku sku, Money price, Money cost, Money salePrice,
                                 String image, BigDecimal weight, Quantity quantity) {
        VariantStatus initial = quantity.value() == 0 ? VariantStatus.OUT_OF_STOCK : VariantStatus.ACTIVE;
        return new Variant(null, productId, sku, price, cost, salePrice, image, weight, quantity, initial, null);
    }

    /** Overload for variants attached to a not-yet-persisted product. */
    public static Variant create(Sku sku, Money price, Money cost, Money salePrice,
                                 String image, BigDecimal weight, Quantity quantity) {
        return create(null, sku, price, cost, salePrice, image, weight, quantity);
    }

    public static Variant reconstitute(VariantId id, ProductId productId, Sku sku, Money price, Money cost,
                                       Money salePrice, String image, BigDecimal weight, Quantity quantity,
                                       VariantStatus status, Long version, List<VariantAttr> attrs) {
        Variant v = new Variant(id, productId, sku, price, cost, salePrice, image, weight, quantity, status, version);
        if (attrs != null) v.attrs.addAll(attrs);
        return v;
    }

    public void assignId(VariantId id) {
        if (this.id != null) throw new IllegalStateException("Variant id already set: " + this.id);
        this.id = Objects.requireNonNull(id, "id");
    }

    public void assignProductId(ProductId productId) {
        Objects.requireNonNull(productId, "productId");
        if (this.productId != null && !this.productId.equals(productId)) {
            throw new IllegalStateException(
                "Variant already belongs to product " + this.productId.value());
        }
        this.productId = productId;
    }

    // ── Mutators ─────────────────────────────────────────────────────────────

    /** Partial-update pricing. {@code null} preserves the existing value; use
     *  {@link #clearSalePrice()} to remove an existing sale price. */
    public void updatePricing(Money price, Money cost, Money salePrice) {
        if (price != null) this.price = price;
        if (cost != null) this.cost = cost;
        if (salePrice != null) this.salePrice = salePrice;
        validateSalePrice();
    }

    public void clearSalePrice() { this.salePrice = null; }

    public void updateStock(Quantity next) {
        this.quantity = Objects.requireNonNull(next, "next");
        reconcileStatusAfterStockChange();
    }

    public void adjustStock(int delta) {
        if (delta == Integer.MIN_VALUE) {
            throw new IllegalArgumentException("delta overflow: Integer.MIN_VALUE is not allowed");
        }
        this.quantity = delta >= 0 ? quantity.add(delta) : quantity.subtract(-delta);
        reconcileStatusAfterStockChange();
    }

    public void activate() {
        this.status = quantity.value() == 0 ? VariantStatus.OUT_OF_STOCK : VariantStatus.ACTIVE;
    }

    public void deactivate() { this.status = VariantStatus.INACTIVE; }

    public void updateImage(String image) { this.image = image; }

    public void updateWeight(BigDecimal weight) { this.weight = weight; }

    public void addAttr(VariantAttr attr) {
        Objects.requireNonNull(attr, "attr");
        if (attrs.stream().anyMatch(a -> a.getAttrId().equals(attr.getAttrId()))) {
            throw new IllegalArgumentException("Variant already has attribute " + attr.getAttrCode());
        }
        attrs.add(attr);
    }

    public void clearAttrs() { attrs.clear(); }

    // ── Queries ──────────────────────────────────────────────────────────────

    /** {@code salePrice} if cheaper than {@code price}, else {@code price}. */
    public Money getEffectivePrice() {
        return salePrice != null && salePrice.isLessThan(price) ? salePrice : price;
    }

    public boolean isAvailable() {
        return status.canSell() && quantity.value() > 0;
    }

    public List<VariantAttr> getAttrs() {
        return Collections.unmodifiableList(attrs);
    }

    private void reconcileStatusAfterStockChange() {
        if (quantity.value() == 0) {
            if (status == VariantStatus.ACTIVE) status = VariantStatus.OUT_OF_STOCK;
        } else if (status == VariantStatus.OUT_OF_STOCK) {
            status = VariantStatus.ACTIVE;
        }
    }

    private void validateSalePrice() {
        if (salePrice != null && salePrice.isGreaterThan(price)) {
            throw new IllegalArgumentException(
                "Sale price " + salePrice.amount() + " must not exceed regular price " + price.amount());
        }
    }
}
