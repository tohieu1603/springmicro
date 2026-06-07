package com.hieu.catalog_service.infrastructure.persistence.mapper;

import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrId;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryId;
import com.hieu.catalog_service.domain.model.product.Product;
import com.hieu.catalog_service.domain.model.product.Variant;
import com.hieu.catalog_service.domain.model.product.VariantAttr;
import com.hieu.catalog_service.domain.model.product.valueobject.Money;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductId;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductStatus;
import com.hieu.catalog_service.domain.model.product.valueobject.Quantity;
import com.hieu.catalog_service.domain.model.product.valueobject.Sku;
import com.hieu.catalog_service.domain.model.product.valueobject.Slug;
import com.hieu.catalog_service.domain.model.product.valueobject.VariantId;
import com.hieu.catalog_service.domain.model.product.valueobject.VariantStatus;
import com.hieu.catalog_service.infrastructure.persistence.jpa.entities.ProductJpaEntity;
import com.hieu.catalog_service.infrastructure.persistence.jpa.entities.VariantAttrJpaEntity;
import com.hieu.catalog_service.infrastructure.persistence.jpa.entities.VariantJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test for the hand-written Product aggregate ↔ JPA entity mapper.
 * Uses a real Jackson {@link JsonMapper} (the image-JSON path is pure, no DB) and asserts:
 * field mapping both directions, images JSON round-trip + null/empty handling, variant +
 * variant-attr reconciliation (insert / update-in-place by id / delete), and
 * {@code syncGeneratedIds} id back-fill.
 */
@DisplayName("ProductJpaMapper — unit")
class ProductJpaMapperTest {

    private final ProductJpaMapper mapper = new ProductJpaMapper(JsonMapper.builder().build());

    private static Variant variant(String id, String productId, String sku, String price, Long version,
                                   List<VariantAttr> attrs) {
        return Variant.reconstitute(
            id != null ? VariantId.of(id) : null,
            productId != null ? ProductId.of(productId) : null,
            Sku.of(sku), Money.of(new BigDecimal(price)), null, null,
            "img.png", new BigDecimal("1.5"), Quantity.of(4),
            VariantStatus.ACTIVE, version, attrs);
    }

    private static Product product(String id, List<Variant> variants) {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        return Product.reconstitute(
            id != null ? ProductId.of(id) : null,
            "Cool Tee", Slug.of("cool-tee"), "A nice tee",
            CategoryId.of("99"), "Brand", "thumb.png",
            List.of("a.png", "b.png"), ProductStatus.ACTIVE,
            "Meta Title", "Meta Desc", "kw1,kw2",
            now, now, "creator", "updater", 3L, variants);
    }

    @Nested
    @DisplayName("toJpa")
    class ToJpa {

        @Test
        @DisplayName("maps all scalar fields and serialises images to JSON")
        void mapsScalars() {
            var p = product("1", List.of(variant("10", "1", "SKU-1", "9.99", 1L, List.of())));

            ProductJpaEntity e = mapper.toJpa(p, null);

            assertThat(e.getName()).isEqualTo("Cool Tee");
            assertThat(e.getSlug()).isEqualTo("cool-tee");
            assertThat(e.getDescription()).isEqualTo("A nice tee");
            assertThat(e.getCategoryId()).isEqualTo("99");
            assertThat(e.getBrand()).isEqualTo("Brand");
            assertThat(e.getThumbnail()).isEqualTo("thumb.png");
            assertThat(e.getImages()).isEqualTo("[\"a.png\",\"b.png\"]");
            assertThat(e.getStatus()).isEqualTo("ACTIVE");
            assertThat(e.getMetaTitle()).isEqualTo("Meta Title");
            assertThat(e.getMetaDescription()).isEqualTo("Meta Desc");
            assertThat(e.getMetaKeywords()).isEqualTo("kw1,kw2");
            assertThat(e.getCreatedBy()).isEqualTo("creator");
            assertThat(e.getUpdatedBy()).isEqualTo("updater");
        }

        @Test
        @DisplayName("null category and empty images → null columns")
        void nullCategoryAndEmptyImages() {
            Instant now = Instant.now();
            var p = Product.reconstitute(ProductId.of("2"), "X", Slug.of("x"), null,
                null, null, null, List.of(), ProductStatus.DRAFT,
                null, null, null, now, now, "c", "c", null, List.of());

            ProductJpaEntity e = mapper.toJpa(p, null);

            assertThat(e.getCategoryId()).isNull();
            assertThat(e.getImages()).isNull();
        }

        @Test
        @DisplayName("reuses existing entity instance (in-place update) when provided")
        void reusesExistingEntity() {
            var existing = new ProductJpaEntity();
            existing.setId("5");

            ProductJpaEntity e = mapper.toJpa(product("5", List.of(variant("10", "5", "SKU-1", "9.99", 1L, List.of()))), existing);

            assertThat(e).isSameAs(existing);
            assertThat(e.getId()).isEqualTo("5");
        }
    }

    @Nested
    @DisplayName("variant reconciliation")
    class Reconciliation {

        @Test
        @DisplayName("new variant (null id) becomes a fresh entity wired to the product")
        void insertsNewVariant() {
            var p = product("1", List.of(variant(null, null, "SKU-NEW", "5.00", null, List.of())));

            ProductJpaEntity e = mapper.toJpa(p, null);

            assertThat(e.getVariants()).singleElement().satisfies(v -> {
                assertThat(v.getId()).isNull();
                assertThat(v.getSku()).isEqualTo("SKU-NEW");
                assertThat(v.getProduct()).isSameAs(e);
                assertThat(v.getPrice()).isEqualByComparingTo("5.00");
            });
        }

        @Test
        @DisplayName("existing variant id is matched and the same entity instance is mutated in place")
        void updatesExistingVariantInPlace() {
            var existing = new ProductJpaEntity();
            existing.setId("1");
            var existingVariant = new VariantJpaEntity();
            existingVariant.setId("10");
            existingVariant.setSku("OLD");
            existingVariant.setPrice(new BigDecimal("1.00"));
            existingVariant.setProduct(existing);
            existing.getVariants().add(existingVariant);

            var p = product("1", List.of(variant("10", "1", "SKU-UPDATED", "7.77", 1L, List.of())));
            ProductJpaEntity e = mapper.toJpa(p, existing);

            assertThat(e.getVariants()).singleElement().satisfies(v -> {
                assertThat(v).isSameAs(existingVariant);
                assertThat(v.getSku()).isEqualTo("SKU-UPDATED");
                assertThat(v.getPrice()).isEqualByComparingTo("7.77");
            });
        }

        @Test
        @DisplayName("variant absent from domain is dropped from the managed collection (orphan removal)")
        void removesMissingVariant() {
            var existing = new ProductJpaEntity();
            existing.setId("1");
            var keep = new VariantJpaEntity();   keep.setId("10"); keep.setSku("KEEP"); keep.setProduct(existing);
            var drop = new VariantJpaEntity();   drop.setId("11"); drop.setSku("DROP"); drop.setProduct(existing);
            existing.getVariants().add(keep);
            existing.getVariants().add(drop);

            var p = product("1", List.of(variant("10", "1", "KEEP", "9.99", 1L, List.of())));
            ProductJpaEntity e = mapper.toJpa(p, existing);

            assertThat(e.getVariants()).extracting(VariantJpaEntity::getSku).containsExactly("KEEP");
        }

        @Test
        @DisplayName("variant attrs are mapped and wired to the variant entity")
        void mapsVariantAttrs() {
            var attr = VariantAttr.create(AttrId.of("7"), "COLOR", "Color", "3", "Red");
            var p = product("1", List.of(variant(null, null, "SKU-A", "5.00", null, List.of(attr))));

            ProductJpaEntity e = mapper.toJpa(p, null);

            VariantJpaEntity v = e.getVariants().get(0);
            assertThat(v.getAttrs()).singleElement().satisfies(a -> {
                assertThat(a.getAttrId()).isEqualTo("7");
                assertThat(a.getAttrCode()).isEqualTo("COLOR");
                assertThat(a.getAttrName()).isEqualTo("Color");
                assertThat(a.getValId()).isEqualTo("3");
                assertThat(a.getValText()).isEqualTo("Red");
                assertThat(a.getVariant()).isSameAs(v);
            });
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ToDomain {

        @Test
        @DisplayName("round-trips a product with a variant (toJpa → toDomain preserves fields)")
        void roundTrip() {
            var attr = VariantAttr.reconstitute("2", AttrId.of("7"), "COLOR", "Color", "3", "Red");
            var original = product("1", List.of(variant("10", "1", "SKU-1", "19.99", 1L, List.of(attr))));

            ProductJpaEntity e = mapper.toJpa(original, null);
            e.setId("1");
            // toJpa builds fresh child entities without ids; reconstitute() requires non-null
            // ids, so simulate the post-flush id assignment before reading back.
            VariantJpaEntity ve = e.getVariants().get(0);
            ve.setId("10");
            ve.getAttrs().get(0).setId("2");
            // toDomainVariant reads e.getProduct().getId() — the reconcile already wired it.
            Product back = mapper.toDomain(e);

            assertThat(back.getId().value()).isEqualTo("1");
            assertThat(back.getName()).isEqualTo("Cool Tee");
            assertThat(back.getSlug().value()).isEqualTo("cool-tee");
            assertThat(back.getCategoryId().value()).isEqualTo("99");
            assertThat(back.getStatus()).isEqualTo(ProductStatus.ACTIVE);
            assertThat(back.getImages()).containsExactly("a.png", "b.png");
            assertThat(back.getVariants()).singleElement().satisfies(v -> {
                assertThat(v.getSku().value()).isEqualTo("SKU-1");
                assertThat(v.getPrice().amount()).isEqualByComparingTo("19.99");
                assertThat(v.getAttrs()).singleElement().satisfies(a -> {
                    assertThat(a.getAttrCode()).isEqualTo("COLOR");
                    assertThat(a.getValText()).isEqualTo("Red");
                });
            });
        }

        @Test
        @DisplayName("null/blank images column → empty list")
        void nullImagesDeserialiseToEmptyList() {
            var e = new ProductJpaEntity();
            e.setId("1");
            e.setName("X");
            e.setSlug("x");
            e.setStatus("DRAFT");
            e.setImages(null);

            Product back = mapper.toDomain(e);

            assertThat(back.getImages()).isEmpty();
        }
    }

    @Nested
    @DisplayName("syncGeneratedIds")
    class SyncGeneratedIds {

        @Test
        @DisplayName("back-fills product id, variant id and variant-attr id by sku/code match")
        void backfillsIds() {
            var attr = VariantAttr.create(AttrId.of("7"), "COLOR", "Color", "3", "Red");
            var aggregate = product(null, List.of(variant(null, null, "SKU-1", "5.00", null, List.of(attr))));

            var saved = new ProductJpaEntity();
            saved.setId("100");
            var savedVariant = new VariantJpaEntity();
            savedVariant.setId("200");
            savedVariant.setSku("SKU-1");
            savedVariant.setProduct(saved);
            var savedAttr = new VariantAttrJpaEntity();
            savedAttr.setId("300");
            savedAttr.setAttrCode("COLOR");
            savedAttr.setVariant(savedVariant);
            savedVariant.getAttrs().add(savedAttr);
            saved.getVariants().add(savedVariant);

            mapper.syncGeneratedIds(aggregate, saved);

            assertThat(aggregate.getId().value()).isEqualTo("100");
            Variant v = aggregate.getVariants().get(0);
            assertThat(v.getId().value()).isEqualTo("200");
            assertThat(v.getProductId().value()).isEqualTo("100");
            assertThat(v.getAttrs().get(0).getId()).isEqualTo("300");
        }

        @Test
        @DisplayName("variant whose sku has no saved match is left untouched")
        void unmatchedVariantSkipped() {
            var aggregate = product(null, List.of(variant(null, null, "SKU-UNMATCHED", "5.00", null, List.of())));
            var saved = new ProductJpaEntity();
            saved.setId("100");
            // no variants in saved

            mapper.syncGeneratedIds(aggregate, saved);

            assertThat(aggregate.getId().value()).isEqualTo("100");
            assertThat(aggregate.getVariants().get(0).getId()).isNull();
        }
    }
}
