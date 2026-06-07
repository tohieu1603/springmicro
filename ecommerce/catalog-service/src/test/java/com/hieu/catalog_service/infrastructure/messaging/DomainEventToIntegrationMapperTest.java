package com.hieu.catalog_service.infrastructure.messaging;

import com.hieu.catalog_service.application.events.AttrIntegrationEvents;
import com.hieu.catalog_service.application.events.CategoryIntegrationEvents;
import com.hieu.catalog_service.application.events.KafkaTopics;
import com.hieu.catalog_service.application.events.ProductIntegrationEvents;
import com.hieu.catalog_service.domain.events.DomainEvent;
import com.hieu.catalog_service.domain.events.attribute.AttrCreatedEvent;
import com.hieu.catalog_service.domain.events.attribute.AttrDeletedEvent;
import com.hieu.catalog_service.domain.events.attribute.AttrUpdatedEvent;
import com.hieu.catalog_service.domain.events.category.CategoryCreatedEvent;
import com.hieu.catalog_service.domain.events.category.CategoryDeletedEvent;
import com.hieu.catalog_service.domain.events.category.CategoryUpdatedEvent;
import com.hieu.catalog_service.domain.events.product.ProductCreatedEvent;
import com.hieu.catalog_service.domain.events.product.ProductDeletedEvent;
import com.hieu.catalog_service.domain.events.product.ProductStatusChangedEvent;
import com.hieu.catalog_service.domain.events.product.ProductUpdatedEvent;
import com.hieu.catalog_service.domain.events.product.VariantAddedEvent;
import com.hieu.catalog_service.domain.events.product.VariantPriceChangedEvent;
import com.hieu.catalog_service.domain.events.product.VariantRemovedEvent;
import com.hieu.catalog_service.domain.events.product.VariantStockChangedEvent;
import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrType;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test for the domain-event → Kafka-integration-event mapper. Verifies the
 * exhaustive switch: topic routing, partition key (aggregateId), payload field mapping,
 * and the {@code null} drop for unmapped events. No Spring, Kafka, or DB.
 */
@DisplayName("DomainEventToIntegrationMapper — unit")
class DomainEventToIntegrationMapperTest {

    private final DomainEventToIntegrationMapper mapper = new DomainEventToIntegrationMapper();

    @Nested
    @DisplayName("product events")
    class ProductEvents {

        @Test
        @DisplayName("ProductCreatedEvent → catalog.product-created with variant snapshots")
        void productCreated() {
            var variant = new ProductCreatedEvent.VariantInfo("10", "SKU-1", new BigDecimal("9.99"), 5);
            var event = new ProductCreatedEvent("1", "Tee", "tee", "desc", "Brand", "DRAFT",
                "thumb.png", "99", "admin", List.of(variant));

            var routed = mapper.map(event);

            assertThat(routed.topic()).isEqualTo(KafkaTopics.PRODUCT_CREATED);
            assertThat(routed.key()).isEqualTo("1");
            assertThat(routed.payload()).isInstanceOf(ProductIntegrationEvents.ProductCreated.class);
            var payload = (ProductIntegrationEvents.ProductCreated) routed.payload();
            assertThat(payload.eventId()).isEqualTo(event.eventId());
            assertThat(payload.occurredOn()).isEqualTo(event.occurredOn());
            assertThat(payload.productId()).isEqualTo("1");
            assertThat(payload.name()).isEqualTo("Tee");
            assertThat(payload.slug()).isEqualTo("tee");
            assertThat(payload.description()).isEqualTo("desc");
            assertThat(payload.brand()).isEqualTo("Brand");
            assertThat(payload.status()).isEqualTo("DRAFT");
            assertThat(payload.thumbnail()).isEqualTo("thumb.png");
            assertThat(payload.categoryId()).isEqualTo("99");
            assertThat(payload.createdBy()).isEqualTo("admin");
            assertThat(payload.variants()).singleElement().satisfies(v -> {
                assertThat(v.variantId()).isEqualTo("10");
                assertThat(v.sku()).isEqualTo("SKU-1");
                assertThat(v.price()).isEqualByComparingTo("9.99");
                assertThat(v.quantity()).isEqualTo(5);
            });
        }

        @Test
        @DisplayName("ProductUpdatedEvent → catalog.product-updated")
        void productUpdated() {
            var event = new ProductUpdatedEvent("2", "New", "new-slug", "editor");

            var routed = mapper.map(event);

            assertThat(routed.topic()).isEqualTo(KafkaTopics.PRODUCT_UPDATED);
            assertThat(routed.key()).isEqualTo("2");
            var payload = (ProductIntegrationEvents.ProductUpdated) routed.payload();
            assertThat(payload.productId()).isEqualTo("2");
            assertThat(payload.name()).isEqualTo("New");
            assertThat(payload.slug()).isEqualTo("new-slug");
            assertThat(payload.updatedBy()).isEqualTo("editor");
        }

        @Test
        @DisplayName("ProductStatusChangedEvent → maps enum status names")
        void productStatusChanged() {
            var event = new ProductStatusChangedEvent("3", ProductStatus.DRAFT, ProductStatus.ACTIVE, "editor");

            var routed = mapper.map(event);

            assertThat(routed.topic()).isEqualTo(KafkaTopics.PRODUCT_STATUS_CHANGED);
            assertThat(routed.key()).isEqualTo("3");
            var payload = (ProductIntegrationEvents.ProductStatusChanged) routed.payload();
            assertThat(payload.oldStatus()).isEqualTo("DRAFT");
            assertThat(payload.newStatus()).isEqualTo("ACTIVE");
            assertThat(payload.updatedBy()).isEqualTo("editor");
        }

        @Test
        @DisplayName("ProductDeletedEvent → carries variant ids")
        void productDeleted() {
            var event = new ProductDeletedEvent("4", List.of("11", "12"), "admin");

            var routed = mapper.map(event);

            assertThat(routed.topic()).isEqualTo(KafkaTopics.PRODUCT_DELETED);
            assertThat(routed.key()).isEqualTo("4");
            var payload = (ProductIntegrationEvents.ProductDeleted) routed.payload();
            assertThat(payload.productId()).isEqualTo("4");
            assertThat(payload.variantIds()).containsExactly("11", "12");
            assertThat(payload.deletedBy()).isEqualTo("admin");
        }
    }

    @Nested
    @DisplayName("variant events")
    class VariantEvents {

        @Test
        @DisplayName("VariantAddedEvent → catalog.variant-added")
        void variantAdded() {
            var event = new VariantAddedEvent("5", "50", "SKU-A", new BigDecimal("12.50"), 7, "admin");

            var routed = mapper.map(event);

            assertThat(routed.topic()).isEqualTo(KafkaTopics.VARIANT_ADDED);
            assertThat(routed.key()).isEqualTo("5");
            var payload = (ProductIntegrationEvents.VariantAdded) routed.payload();
            assertThat(payload.productId()).isEqualTo("5");
            assertThat(payload.variantId()).isEqualTo("50");
            assertThat(payload.sku()).isEqualTo("SKU-A");
            assertThat(payload.price()).isEqualByComparingTo("12.50");
            assertThat(payload.quantity()).isEqualTo(7);
            assertThat(payload.createdBy()).isEqualTo("admin");
        }

        @Test
        @DisplayName("VariantRemovedEvent → catalog.variant-removed")
        void variantRemoved() {
            var event = new VariantRemovedEvent("6", "60", "SKU-B", "admin");

            var routed = mapper.map(event);

            assertThat(routed.topic()).isEqualTo(KafkaTopics.VARIANT_REMOVED);
            assertThat(routed.key()).isEqualTo("6");
            var payload = (ProductIntegrationEvents.VariantRemoved) routed.payload();
            assertThat(payload.variantId()).isEqualTo("60");
            assertThat(payload.sku()).isEqualTo("SKU-B");
            assertThat(payload.deletedBy()).isEqualTo("admin");
        }

        @Test
        @DisplayName("VariantStockChangedEvent → computes delta")
        void variantStockChanged() {
            var event = new VariantStockChangedEvent("7", "70", "SKU-C", 10, 3, "system");

            var routed = mapper.map(event);

            assertThat(routed.topic()).isEqualTo(KafkaTopics.VARIANT_STOCK_CHANGED);
            assertThat(routed.key()).isEqualTo("7");
            var payload = (ProductIntegrationEvents.VariantStockChanged) routed.payload();
            assertThat(payload.oldQuantity()).isEqualTo(10);
            assertThat(payload.newQuantity()).isEqualTo(3);
            assertThat(payload.delta()).isEqualTo(-7);
            assertThat(payload.updatedBy()).isEqualTo("system");
        }

        @Test
        @DisplayName("VariantPriceChangedEvent → maps old/new/sale prices")
        void variantPriceChanged() {
            var event = new VariantPriceChangedEvent("8", "80", "SKU-D",
                new BigDecimal("20.00"), new BigDecimal("18.00"), new BigDecimal("15.00"), "admin");

            var routed = mapper.map(event);

            assertThat(routed.topic()).isEqualTo(KafkaTopics.VARIANT_PRICE_CHANGED);
            assertThat(routed.key()).isEqualTo("8");
            var payload = (ProductIntegrationEvents.VariantPriceChanged) routed.payload();
            assertThat(payload.oldPrice()).isEqualByComparingTo("20.00");
            assertThat(payload.newPrice()).isEqualByComparingTo("18.00");
            assertThat(payload.newSalePrice()).isEqualByComparingTo("15.00");
        }
    }

    @Nested
    @DisplayName("category events")
    class CategoryEvents {

        @Test
        @DisplayName("CategoryCreatedEvent → catalog.category-created")
        void categoryCreated() {
            var event = new CategoryCreatedEvent("9", "Shoes", "1", "admin");

            var routed = mapper.map(event);

            assertThat(routed.topic()).isEqualTo(KafkaTopics.CATEGORY_CREATED);
            assertThat(routed.key()).isEqualTo("9");
            var payload = (CategoryIntegrationEvents.CategoryCreated) routed.payload();
            assertThat(payload.categoryId()).isEqualTo("9");
            assertThat(payload.name()).isEqualTo("Shoes");
            assertThat(payload.parentId()).isEqualTo("1");
            assertThat(payload.createdBy()).isEqualTo("admin");
        }

        @Test
        @DisplayName("CategoryUpdatedEvent → catalog.category-updated")
        void categoryUpdated() {
            var event = new CategoryUpdatedEvent("10", "Boots", null, "editor");

            var routed = mapper.map(event);

            assertThat(routed.topic()).isEqualTo(KafkaTopics.CATEGORY_UPDATED);
            assertThat(routed.key()).isEqualTo("10");
            var payload = (CategoryIntegrationEvents.CategoryUpdated) routed.payload();
            assertThat(payload.name()).isEqualTo("Boots");
            assertThat(payload.parentId()).isNull();
            assertThat(payload.updatedBy()).isEqualTo("editor");
        }

        @Test
        @DisplayName("CategoryDeletedEvent → catalog.category-deleted")
        void categoryDeleted() {
            var event = new CategoryDeletedEvent("11", "admin");

            var routed = mapper.map(event);

            assertThat(routed.topic()).isEqualTo(KafkaTopics.CATEGORY_DELETED);
            assertThat(routed.key()).isEqualTo("11");
            var payload = (CategoryIntegrationEvents.CategoryDeleted) routed.payload();
            assertThat(payload.categoryId()).isEqualTo("11");
            assertThat(payload.deletedBy()).isEqualTo("admin");
        }
    }

    @Nested
    @DisplayName("attribute events")
    class AttrEvents {

        @Test
        @DisplayName("AttrCreatedEvent → maps enum type name")
        void attrCreated() {
            var event = new AttrCreatedEvent("12", "COLOR", "Color", AttrType.SELECT);

            var routed = mapper.map(event);

            assertThat(routed.topic()).isEqualTo(KafkaTopics.ATTR_CREATED);
            assertThat(routed.key()).isEqualTo("12");
            var payload = (AttrIntegrationEvents.AttrCreated) routed.payload();
            assertThat(payload.attrId()).isEqualTo("12");
            assertThat(payload.code()).isEqualTo("COLOR");
            assertThat(payload.name()).isEqualTo("Color");
            assertThat(payload.type()).isEqualTo("SELECT");
        }

        @Test
        @DisplayName("AttrUpdatedEvent → catalog.attr-updated")
        void attrUpdated() {
            var event = new AttrUpdatedEvent("13", "Colour");

            var routed = mapper.map(event);

            assertThat(routed.topic()).isEqualTo(KafkaTopics.ATTR_UPDATED);
            assertThat(routed.key()).isEqualTo("13");
            var payload = (AttrIntegrationEvents.AttrUpdated) routed.payload();
            assertThat(payload.attrId()).isEqualTo("13");
            assertThat(payload.name()).isEqualTo("Colour");
        }

        @Test
        @DisplayName("AttrDeletedEvent → catalog.attr-deleted")
        void attrDeleted() {
            var event = new AttrDeletedEvent("14");

            var routed = mapper.map(event);

            assertThat(routed.topic()).isEqualTo(KafkaTopics.ATTR_DELETED);
            assertThat(routed.key()).isEqualTo("14");
            var payload = (AttrIntegrationEvents.AttrDeleted) routed.payload();
            assertThat(payload.attrId()).isEqualTo("14");
        }
    }

    @Test
    @DisplayName("unmapped domain event → null (silently dropped, not leaked externally)")
    void unmappedEvent_returnsNull() {
        DomainEvent unknown = new DomainEvent() {
            @Override public String aggregateId() { return "x"; }
        };

        assertThat(mapper.map(unknown)).isNull();
    }
}
