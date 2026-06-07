package com.hieu.catalog_service.application.events;

/** Kafka topic names for catalog integration events. Single source of truth. */
public final class KafkaTopics {

    private KafkaTopics() {}

    public static final String PRODUCT_CREATED          = "catalog.product-created";
    public static final String PRODUCT_UPDATED          = "catalog.product-updated";
    public static final String PRODUCT_STATUS_CHANGED   = "catalog.product-status-changed";
    public static final String PRODUCT_DELETED          = "catalog.product-deleted";

    public static final String VARIANT_ADDED            = "catalog.variant-added";
    public static final String VARIANT_REMOVED          = "catalog.variant-removed";
    public static final String VARIANT_STOCK_CHANGED    = "catalog.variant-stock-changed";
    public static final String VARIANT_PRICE_CHANGED    = "catalog.variant-price-changed";

    public static final String CATEGORY_CREATED         = "catalog.category-created";
    public static final String CATEGORY_UPDATED         = "catalog.category-updated";
    public static final String CATEGORY_DELETED         = "catalog.category-deleted";

    public static final String ATTR_CREATED             = "catalog.attr-created";
    public static final String ATTR_UPDATED             = "catalog.attr-updated";
    public static final String ATTR_DELETED             = "catalog.attr-deleted";
}
