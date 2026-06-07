package com.hieu.order_service.infrastructure.grpc.client;

import com.hieu.catalog_service.interfaces.grpc.proto.CatalogServiceGrpc;
import com.hieu.catalog_service.interfaces.grpc.proto.CheckStockRequest;
import com.hieu.catalog_service.interfaces.grpc.proto.CheckStockResponse;
import com.hieu.catalog_service.interfaces.grpc.proto.GetProductRequest;
import com.hieu.catalog_service.interfaces.grpc.proto.GetVariantBySkuRequest;
import com.hieu.catalog_service.interfaces.grpc.proto.Variant;
import com.hieu.order_service.domain.exception.ServiceUnavailableException;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * gRPC client for catalog-service — powers the saga's price-verification step.
 *
 * <p>Transport errors surface as {@link ServiceUnavailableException}. Logical "not found"
 * (bool {@code found = false} on the response) returns {@link Optional#empty()} so the
 * caller can decide whether to fail the saga or retry.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogGrpcClient {

    /** Per-call deadline — without this, a hung catalog-service blocks the caller's
     *  thread (Tomcat / Kafka consumer / sagaExecutor) indefinitely. 2s covers the
     *  p99 latency of the saga's price-verification fan-out. */
    private static final long DEADLINE_MS = 2000;

    private final CatalogServiceGrpc.CatalogServiceBlockingStub stub;

    private CatalogServiceGrpc.CatalogServiceBlockingStub deadlined() {
        return stub.withDeadlineAfter(DEADLINE_MS, TimeUnit.MILLISECONDS);
    }

    public Optional<ProductSnapshot> getProduct(String productId) {
        try {
            var resp = deadlined().getProduct(GetProductRequest.newBuilder().setProductId(productId).build());
            if (!resp.getFound()) return Optional.empty();
            var p = resp.getProduct();
            var variants = p.getVariantsList().stream()
                    .map(v -> new VariantSnapshot(v.getId(), v.getSku(),
                            parsePrice(v.getPrice()), v.getQuantity(), v.getStatus()))
                    .toList();
            return Optional.of(new ProductSnapshot(p.getId(), p.getName(), p.getStatus(), variants));
        } catch (StatusRuntimeException e) {
            log.error("gRPC getProduct({}) transport error: {}", productId, e.getStatus().getCode());
            throw new ServiceUnavailableException("catalog-service");
        }
    }

    public Optional<VariantSnapshot> getVariantBySku(String sku) {
        try {
            var resp = deadlined().getVariantBySku(GetVariantBySkuRequest.newBuilder().setSku(sku).build());
            if (!resp.getFound()) return Optional.empty();
            var v = resp.getVariant();
            return Optional.of(new VariantSnapshot(v.getId(), v.getSku(),
                    parsePrice(v.getPrice()), v.getQuantity(), v.getStatus()));
        } catch (StatusRuntimeException e) {
            log.error("gRPC getVariantBySku({}) transport error: {}", sku, e.getStatus().getCode());
            throw new ServiceUnavailableException("catalog-service");
        }
    }

    /** Catalog-side stock is an advisory snapshot; inventory-service remains authoritative. */
    public boolean checkStock(String sku, int requested) {
        try {
            CheckStockResponse resp = deadlined().checkStock(CheckStockRequest.newBuilder()
                    .setSku(sku).setRequested(requested).build());
            return resp.getAvailable();
        } catch (StatusRuntimeException e) {
            log.warn("gRPC catalog.checkStock({}) failed: {}", sku, e.getMessage());
            return false;
        }
    }

    private static BigDecimal parsePrice(String s) {
        if (s == null || s.isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(s); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

    public record VariantSnapshot(String id, String sku, BigDecimal price, int quantity, String status) {}
    public record ProductSnapshot(String id, String name, String status, List<VariantSnapshot> variants) {}
}
