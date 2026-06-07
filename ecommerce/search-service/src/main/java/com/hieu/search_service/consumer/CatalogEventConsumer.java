package com.hieu.search_service.consumer;

import com.hieu.search_service.dto.IndexProductRequest;
import com.hieu.search_service.service.SearchApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Listens to catalog domain events and keeps the Elasticsearch index in sync.
 * Payload is deserialized as Map<String,Object> (no type headers).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogEventConsumer {

    private static final String GROUP = "search-service";

    private final SearchApplicationService searchService;

    @KafkaListener(topics = "catalog.product-created", groupId = GROUP)
    public void onProductCreated(Map<String, Object> payload) {
        log.debug("Received catalog.product-created: id={}", payload.get("id"));
        searchService.indexProduct(toRequest(payload));
    }

    @KafkaListener(topics = "catalog.product-updated", groupId = GROUP)
    public void onProductUpdated(Map<String, Object> payload) {
        log.debug("Received catalog.product-updated: id={}", payload.get("id"));
        searchService.indexProduct(toRequest(payload));
    }

    @KafkaListener(topics = "catalog.product-status-changed", groupId = GROUP)
    public void onProductStatusChanged(Map<String, Object> payload) {
        // Status-changed payload only carries productId+old/new status — calling
        // indexProduct() here would null out all other fields. Use a partial update
        // so existing name/brand/price stay intact.
        String id = getString(payload, "id");
        if (id == null) id = getString(payload, "productId");
        String newStatus = getString(payload, "newStatus");
        if (id == null || newStatus == null) {
            log.warn("status-changed missing id/newStatus: {}", payload);
            return;
        }
        log.debug("Partial update product status id={} -> {}", id, newStatus);
        searchService.updateStatus(id, newStatus);
    }

    @KafkaListener(topics = "catalog.product-deleted", groupId = GROUP)
    public void onProductDeleted(Map<String, Object> payload) {
        String id = getString(payload, "id");
        log.debug("Received catalog.product-deleted: id={}", id);
        if (id != null) searchService.removeProduct(id);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Maps the catalog event payload (nested productId/variants) to an index request.
     * Catalog event uses {@code productId} (Long) and a {@code variants} list with
     * {@code price, quantity}. We aggregate min/max price + total stock here so the
     * search-service doesn't need to call back to catalog.
     */
    private static IndexProductRequest toRequest(Map<String, Object> p) {
        // Catalog event field is "productId", not "id". Search index id is its String form.
        String id = firstNonNull(getString(p, "id"), getString(p, "productId"));

        VariantAggregates agg = aggregateVariants(p.get("variants"));

        return IndexProductRequest.builder()
                .id(id)
                .name(getString(p, "name"))
                .description(getString(p, "description"))
                .sku(getString(p, "sku"))
                .categoryId(getString(p, "categoryId"))
                .categoryName(getString(p, "categoryName"))
                .brand(getString(p, "brand"))
                .price(getDouble(p, "price"))
                .minPrice(firstNonNull(agg.minPrice(), getDouble(p, "minPrice")))
                .maxPrice(firstNonNull(agg.maxPrice(), getDouble(p, "maxPrice")))
                .totalStock(firstNonNull(agg.totalStock(), getInt(p, "totalStock")))
                .status(getString(p, "status"))
                .imageUrl(firstNonNull(getString(p, "thumbnail"), getString(p, "imageUrl")))
                .tags(getList(p, "tags"))
                .createdAt(getInstant(p, "createdAt"))
                .updatedAt(getInstant(p, "updatedAt"))
                .build();
    }

    /** Aggregated variant stats — null fields mean "not present in payload". */
    private record VariantAggregates(Double minPrice, Double maxPrice, Integer totalStock) {
        static final VariantAggregates EMPTY = new VariantAggregates(null, null, null);
    }

    /** Reduce variants[] → min/max price + total stock. Pure, easy to unit-test. */
    @SuppressWarnings("unchecked")
    private static VariantAggregates aggregateVariants(Object varsRaw) {
        if (!(varsRaw instanceof List<?> vars) || vars.isEmpty()) {
            return VariantAggregates.EMPTY;
        }
        int sumStock = 0;
        double minP = Double.MAX_VALUE;
        double maxP = -Double.MAX_VALUE;
        for (Object v : vars) {
            if (!(v instanceof Map)) continue;
            Map<String, Object> vm = (Map<String, Object>) v;
            Double price = getDouble(vm, "price");
            Integer qty = getInt(vm, "quantity");
            if (price != null) {
                minP = Math.min(minP, price);
                maxP = Math.max(maxP, price);
            }
            if (qty != null) sumStock += qty;
        }
        Double min = minP == Double.MAX_VALUE ? null : minP;
        Double max = maxP == -Double.MAX_VALUE ? null : maxP;
        return new VariantAggregates(min, max, sumStock);
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T v : values) if (v != null) return v;
        return null;
    }

    private static String getString(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : null;
    }

    private static Double getDouble(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (NumberFormatException e) { return null; }
    }

    private static Integer getInt(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return null; }
    }

    @SuppressWarnings("unchecked")
    private static List<String> getList(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof List<?> list) return list.stream().map(Object::toString).toList();
        return List.of();
    }

    private static Instant getInstant(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return Instant.ofEpochMilli(n.longValue());
        try { return Instant.parse(v.toString()); } catch (Exception e) { return null; }
    }
}
