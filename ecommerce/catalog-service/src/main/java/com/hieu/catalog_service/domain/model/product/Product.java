package com.hieu.catalog_service.domain.model.product;

import com.hieu.catalog_service.domain.events.product.ProductCreatedEvent;
import com.hieu.catalog_service.domain.events.product.ProductDeletedEvent;
import com.hieu.catalog_service.domain.events.product.ProductStatusChangedEvent;
import com.hieu.catalog_service.domain.events.product.ProductUpdatedEvent;
import com.hieu.catalog_service.domain.events.product.VariantAddedEvent;
import com.hieu.catalog_service.domain.events.product.VariantPriceChangedEvent;
import com.hieu.catalog_service.domain.events.product.VariantRemovedEvent;
import com.hieu.catalog_service.domain.events.product.VariantStockChangedEvent;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryId;
import com.hieu.catalog_service.domain.model.product.valueobject.Money;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductId;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductStatus;
import com.hieu.catalog_service.domain.model.product.valueobject.Quantity;
import com.hieu.catalog_service.domain.model.product.valueobject.Slug;
import com.hieu.catalog_service.domain.model.product.valueobject.VariantId;
import com.hieu.catalog_service.domain.shared.AggregateRoot;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Product — the SPU aggregate root. Owns its {@link Variant} children (sellable SKUs) and
 * enforces all cross-variant invariants (price range, availability, stock totals).
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>{@link #create} — build a DRAFT product with at least one variant, then register
 *       {@link ProductCreatedEvent} via {@link #raiseCreatedEvent()} after persistence
 *       assigns ids.</li>
 *   <li>Mutations ({@link #update}, status transitions, variant ops) register their event
 *       for AFTER_COMMIT Kafka publication.</li>
 *   <li>{@link #softDelete} flips status to {@link ProductStatus#DELETED} and fires
 *       {@link ProductDeletedEvent}. The row stays for audit/FK integrity.</li>
 * </ol>
 */
@Getter
public final class Product extends AggregateRoot {

    private ProductId id;
    private String name;
    private Slug slug;
    private String description;
    private CategoryId categoryId;
    private String brand;
    private String thumbnail;
    private final List<String> images = new ArrayList<>();
    private ProductStatus status;
    private String metaTitle;
    private String metaDescription;
    private String metaKeywords;
    private Instant createdAt;
    private Instant updatedAt;
    private final String createdBy;
    private String updatedBy;
    private Long version;

    private final List<Variant> variants = new ArrayList<>();

    private Product(ProductId id, String name, Slug slug, String description, CategoryId categoryId,
                    String brand, String thumbnail, List<String> images, ProductStatus status,
                    String metaTitle, String metaDescription, String metaKeywords,
                    Instant createdAt, Instant updatedAt, String createdBy, String updatedBy, Long version) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name");
        this.slug = Objects.requireNonNull(slug, "slug");
        this.description = description;
        this.categoryId = categoryId;
        this.brand = brand;
        this.thumbnail = thumbnail;
        if (images != null) this.images.addAll(images);
        this.status = Objects.requireNonNull(status, "status");
        this.metaTitle = metaTitle;
        this.metaDescription = metaDescription;
        this.metaKeywords = metaKeywords;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.version = version;
    }

    // ── Factories ────────────────────────────────────────────────────────────

    public static Product create(String name, String description, CategoryId categoryId,
                                  String brand, String createdBy) {
        Objects.requireNonNull(name, "name");
        Instant now = Instant.now();
        return new Product(null, name.trim(), Slug.generate(name), description,
            categoryId, brand, null, new ArrayList<>(), ProductStatus.DRAFT,
            null, null, null, now, now, createdBy, createdBy, null);
    }

    public static Product reconstitute(ProductId id, String name, Slug slug, String description,
                                       CategoryId categoryId, String brand, String thumbnail,
                                       List<String> images, ProductStatus status,
                                       String metaTitle, String metaDescription, String metaKeywords,
                                       Instant createdAt, Instant updatedAt,
                                       String createdBy, String updatedBy, Long version,
                                       List<Variant> variants) {
        Product p = new Product(id, name, slug, description, categoryId, brand, thumbnail, images, status,
            metaTitle, metaDescription, metaKeywords, createdAt, updatedAt, createdBy, updatedBy, version);
        if (variants != null) p.variants.addAll(variants);
        return p;
    }

    public void assignId(ProductId id) {
        if (this.id != null) throw new IllegalStateException("Product id already set: " + this.id);
        this.id = Objects.requireNonNull(id, "id");
    }

    public void replaceSlug(Slug slug) {
        this.slug = Objects.requireNonNull(slug, "slug");
    }

    // ── Lifecycle events ─────────────────────────────────────────────────────

    /** Register {@link ProductCreatedEvent}. Callable only AFTER persistence assigns ids. */
    public void raiseCreatedEvent() {
        requirePersisted();
        List<ProductCreatedEvent.VariantInfo> infos = variants.stream()
            .map(v -> new ProductCreatedEvent.VariantInfo(
                Objects.requireNonNull(v.getId(), "variant id must be assigned").value(),
                v.getSku().value(), v.getPrice().amount(), v.getQuantity().value()))
            .toList();
        registerEvent(new ProductCreatedEvent(id.value(), name, slug.value(),
            description, brand, status.name(), thumbnail,
            categoryId != null ? categoryId.value() : null, createdBy, infos));
    }

    // ── Core mutations ───────────────────────────────────────────────────────

    public void update(String name, String description, CategoryId categoryId, String brand, String updatedBy) {
        if (status.isDeleted()) throw new IllegalStateException("Cannot update a deleted product");
        boolean changed = false;
        if (name != null && !name.equals(this.name))               { this.name = name.trim(); changed = true; }
        if (description != null && !description.equals(this.description)) { this.description = description; changed = true; }
        if (categoryId != null && !categoryId.equals(this.categoryId)) { this.categoryId = categoryId; changed = true; }
        if (brand != null && !brand.equals(this.brand))            { this.brand = brand; changed = true; }
        if (!changed) return;
        touch(updatedBy);
        requirePersisted();
        registerEvent(new ProductUpdatedEvent(id.value(), this.name, this.slug.value(), updatedBy));
    }

    public void updateImages(String thumbnail, List<String> images, String updatedBy) {
        if (status.isDeleted()) throw new IllegalStateException("Cannot update a deleted product");
        this.thumbnail = thumbnail;
        this.images.clear();
        if (images != null) this.images.addAll(images);
        touch(updatedBy);
        if (id != null) registerEvent(new ProductUpdatedEvent(id.value(), this.name, this.slug.value(), updatedBy));
    }

    public void updateSeo(String metaTitle, String metaDescription, String metaKeywords, String updatedBy) {
        this.metaTitle = metaTitle;
        this.metaDescription = metaDescription;
        this.metaKeywords = metaKeywords;
        touch(updatedBy);
        if (id != null) registerEvent(new ProductUpdatedEvent(id.value(), this.name, this.slug.value(), updatedBy));
    }

    // ── Status transitions ───────────────────────────────────────────────────

    public void activate(String updatedBy)    { transitionTo(ProductStatus.ACTIVE, updatedBy); }
    public void deactivate(String updatedBy)  { transitionTo(ProductStatus.INACTIVE, updatedBy); }
    public void moveToDraft(String updatedBy) { transitionTo(ProductStatus.DRAFT, updatedBy); }

    private void transitionTo(ProductStatus next, String updatedBy) {
        Objects.requireNonNull(next, "next");
        if (status == next) return;
        if (status.isDeleted()) throw new IllegalStateException("Cannot transition a deleted product to " + next);
        // Guard: ACTIVE products must have at least one variant — prevents publishing empty SPUs.
        if (next == ProductStatus.ACTIVE && variants.isEmpty()) {
            throw new IllegalStateException("Cannot activate a product with no variants");
        }
        ProductStatus prev = status;
        status = next;
        touch(updatedBy);
        if (id != null) registerEvent(new ProductStatusChangedEvent(id.value(), prev, next, updatedBy));
    }

    public void softDelete(String deletedBy) {
        if (status.isDeleted()) return;
        ProductStatus prev = status;
        status = ProductStatus.DELETED;
        touch(deletedBy);
        requirePersisted();
        List<String> variantIds = variants.stream()
            .map(v -> v.getId() != null ? v.getId().value() : null)
            .filter(Objects::nonNull)
            .toList();
        registerEvent(new ProductStatusChangedEvent(id.value(), prev, ProductStatus.DELETED, deletedBy));
        registerEvent(new ProductDeletedEvent(id.value(), variantIds, deletedBy));
    }

    // ── Variant management ──────────────────────────────────────────────────

    public void addVariant(Variant variant) {
        Objects.requireNonNull(variant, "variant");
        if (status.isDeleted()) throw new IllegalStateException("Cannot add variants to a deleted product");
        if (variants.stream().anyMatch(v -> v.getSku().equals(variant.getSku()))) {
            throw new IllegalArgumentException("Duplicate SKU on product: " + variant.getSku().value());
        }
        variants.add(variant);
        if (id != null && variant.getId() != null) {
            registerEvent(new VariantAddedEvent(id.value(), variant.getId().value(),
                variant.getSku().value(), variant.getPrice().amount(), variant.getQuantity().value(), createdBy));
        }
    }

    public void removeVariant(VariantId variantId, String deletedBy) {
        Objects.requireNonNull(variantId, "variantId");
        if (variants.size() == 1 && status == ProductStatus.ACTIVE) {
            throw new IllegalStateException("Cannot remove the last variant of an ACTIVE product");
        }
        Variant removed = variants.stream()
            .filter(v -> variantId.equals(v.getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + variantId.value()));
        variants.remove(removed);
        if (id != null) registerEvent(new VariantRemovedEvent(
            id.value(), removed.getId().value(), removed.getSku().value(), deletedBy));
    }

    public void updateVariantPricing(VariantId variantId, Money price, Money cost, Money salePrice, String updatedBy) {
        Variant v = requireVariant(variantId);
        BigDecimal oldPrice = v.getPrice().amount();
        v.updatePricing(price, cost, salePrice);
        touch(updatedBy);
        if (id != null) registerEvent(new VariantPriceChangedEvent(
            id.value(), v.getId().value(), v.getSku().value(),
            oldPrice, v.getPrice().amount(),
            v.getSalePrice() != null ? v.getSalePrice().amount() : null, updatedBy));
    }

    public void updateVariantStock(VariantId variantId, Quantity quantity, String updatedBy) {
        Variant v = requireVariant(variantId);
        int old = v.getQuantity().value();
        v.updateStock(quantity);
        touch(updatedBy);
        if (id != null) registerEvent(new VariantStockChangedEvent(
            id.value(), v.getId().value(), v.getSku().value(), old, v.getQuantity().value(), updatedBy));
    }

    public void adjustVariantStock(VariantId variantId, int delta, String updatedBy) {
        Variant v = requireVariant(variantId);
        int old = v.getQuantity().value();
        v.adjustStock(delta);
        touch(updatedBy);
        if (id != null) registerEvent(new VariantStockChangedEvent(
            id.value(), v.getId().value(), v.getSku().value(), old, v.getQuantity().value(), updatedBy));
    }

    public void activateVariant(VariantId variantId, String updatedBy)   { requireVariant(variantId).activate(); touch(updatedBy); }
    public void deactivateVariant(VariantId variantId, String updatedBy) { requireVariant(variantId).deactivate(); touch(updatedBy); }

    // ── Queries ──────────────────────────────────────────────────────────────

    public PriceRange getPriceRange() {
        if (variants.isEmpty()) return new PriceRange(BigDecimal.ZERO, BigDecimal.ZERO);
        BigDecimal min = null, max = null;
        for (Variant v : variants) {
            BigDecimal eff = v.getEffectivePrice().amount();
            if (min == null || eff.compareTo(min) < 0) min = eff;
            if (max == null || eff.compareTo(max) > 0) max = eff;
        }
        return new PriceRange(min, max);
    }

    public int getTotalStock() {
        return variants.stream().mapToInt(v -> v.getQuantity().value()).sum();
    }

    public boolean hasAvailableVariant() {
        return variants.stream().anyMatch(Variant::isAvailable);
    }

    public boolean isActive() { return status.isActive(); }

    public List<Variant> getVariants() { return Collections.unmodifiableList(variants); }

    public List<String> getImages() { return Collections.unmodifiableList(images); }

    private Variant requireVariant(VariantId variantId) {
        Objects.requireNonNull(variantId, "variantId");
        return variants.stream()
            .filter(v -> variantId.equals(v.getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + variantId.value()));
    }

    private void requirePersisted() {
        if (id == null) throw new IllegalStateException("Product is not yet persisted");
    }

    private void touch(String by) {
        this.updatedBy = by;
        this.updatedAt = Instant.now();
    }

    public record PriceRange(BigDecimal min, BigDecimal max) {
        public boolean isSinglePrice() { return min.compareTo(max) == 0; }
    }
}
