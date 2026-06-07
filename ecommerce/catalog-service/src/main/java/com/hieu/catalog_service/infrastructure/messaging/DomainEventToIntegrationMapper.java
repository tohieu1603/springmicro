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
import org.springframework.stereotype.Component;

/**
 * Maps in-process {@link DomainEvent}s to Kafka-bound integration event records.
 * Pattern matching keeps the switch exhaustive for the curated set; any domain event
 * not explicitly handled returns {@code null} and is silently dropped — we don't leak
 * every internal event to external consumers.
 */
@Component
public class DomainEventToIntegrationMapper {

    public record Routed(String topic, String key, Object payload) {}

    public Routed map(DomainEvent event) {
        return switch (event) {
            case ProductCreatedEvent e -> new Routed(
                KafkaTopics.PRODUCT_CREATED,
                e.aggregateId(),
                new ProductIntegrationEvents.ProductCreated(
                    e.eventId(), e.occurredOn(),
                    e.getProductId(), e.getName(), e.getSlug(),
                    e.getDescription(), e.getBrand(), e.getStatus(), e.getThumbnail(),
                    e.getCategoryId(), e.getCreatedBy(),
                    e.getVariants().stream()
                        .map(v -> new ProductIntegrationEvents.VariantSnapshot(
                            v.variantId(), v.sku(), v.price(), v.quantity()))
                        .toList()));

            case ProductUpdatedEvent e -> new Routed(
                KafkaTopics.PRODUCT_UPDATED, e.aggregateId(),
                new ProductIntegrationEvents.ProductUpdated(
                    e.eventId(), e.occurredOn(),
                    e.getProductId(), e.getName(), e.getSlug(), e.getUpdatedBy()));

            case ProductStatusChangedEvent e -> new Routed(
                KafkaTopics.PRODUCT_STATUS_CHANGED, e.aggregateId(),
                new ProductIntegrationEvents.ProductStatusChanged(
                    e.eventId(), e.occurredOn(),
                    e.getProductId(), e.getOldStatus().name(), e.getNewStatus().name(), e.getUpdatedBy()));

            case ProductDeletedEvent e -> new Routed(
                KafkaTopics.PRODUCT_DELETED, e.aggregateId(),
                new ProductIntegrationEvents.ProductDeleted(
                    e.eventId(), e.occurredOn(),
                    e.getProductId(), e.getVariantIds(), e.getDeletedBy()));

            case VariantAddedEvent e -> new Routed(
                KafkaTopics.VARIANT_ADDED, e.aggregateId(),
                new ProductIntegrationEvents.VariantAdded(
                    e.eventId(), e.occurredOn(),
                    e.getProductId(), e.getVariantId(), e.getSku(),
                    e.getPrice(), e.getQuantity(), e.getCreatedBy()));

            case VariantRemovedEvent e -> new Routed(
                KafkaTopics.VARIANT_REMOVED, e.aggregateId(),
                new ProductIntegrationEvents.VariantRemoved(
                    e.eventId(), e.occurredOn(),
                    e.getProductId(), e.getVariantId(), e.getSku(), e.getDeletedBy()));

            case VariantStockChangedEvent e -> new Routed(
                KafkaTopics.VARIANT_STOCK_CHANGED, e.aggregateId(),
                new ProductIntegrationEvents.VariantStockChanged(
                    e.eventId(), e.occurredOn(),
                    e.getProductId(), e.getVariantId(), e.getSku(),
                    e.getOldQuantity(), e.getNewQuantity(), e.getDelta(), e.getUpdatedBy()));

            case VariantPriceChangedEvent e -> new Routed(
                KafkaTopics.VARIANT_PRICE_CHANGED, e.aggregateId(),
                new ProductIntegrationEvents.VariantPriceChanged(
                    e.eventId(), e.occurredOn(),
                    e.getProductId(), e.getVariantId(), e.getSku(),
                    e.getOldPrice(), e.getNewPrice(), e.getNewSalePrice(), e.getUpdatedBy()));

            case CategoryCreatedEvent e -> new Routed(
                KafkaTopics.CATEGORY_CREATED, e.aggregateId(),
                new CategoryIntegrationEvents.CategoryCreated(
                    e.eventId(), e.occurredOn(),
                    e.getCategoryId(), e.getName(), e.getParentId(), e.getCreatedBy()));

            case CategoryUpdatedEvent e -> new Routed(
                KafkaTopics.CATEGORY_UPDATED, e.aggregateId(),
                new CategoryIntegrationEvents.CategoryUpdated(
                    e.eventId(), e.occurredOn(),
                    e.getCategoryId(), e.getName(), e.getParentId(), e.getUpdatedBy()));

            case CategoryDeletedEvent e -> new Routed(
                KafkaTopics.CATEGORY_DELETED, e.aggregateId(),
                new CategoryIntegrationEvents.CategoryDeleted(
                    e.eventId(), e.occurredOn(),
                    e.getCategoryId(), e.getDeletedBy()));

            case AttrCreatedEvent e -> new Routed(
                KafkaTopics.ATTR_CREATED, e.aggregateId(),
                new AttrIntegrationEvents.AttrCreated(
                    e.eventId(), e.occurredOn(),
                    e.getAttrId(), e.getCode(), e.getName(), e.getType().name()));

            case AttrUpdatedEvent e -> new Routed(
                KafkaTopics.ATTR_UPDATED, e.aggregateId(),
                new AttrIntegrationEvents.AttrUpdated(
                    e.eventId(), e.occurredOn(),
                    e.getAttrId(), e.getName()));

            case AttrDeletedEvent e -> new Routed(
                KafkaTopics.ATTR_DELETED, e.aggregateId(),
                new AttrIntegrationEvents.AttrDeleted(
                    e.eventId(), e.occurredOn(), e.getAttrId()));

            default -> null;
        };
    }
}
