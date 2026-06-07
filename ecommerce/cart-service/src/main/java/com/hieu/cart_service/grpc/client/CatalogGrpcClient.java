package com.hieu.cart_service.grpc.client;

import com.hieu.catalog_service.interfaces.grpc.proto.CatalogServiceGrpc;
import com.hieu.catalog_service.interfaces.grpc.proto.GetProductRequest;
import com.hieu.catalog_service.interfaces.grpc.proto.GetProductResponse;
import com.hieu.catalog_service.interfaces.grpc.proto.GetVariantBySkuRequest;
import com.hieu.catalog_service.interfaces.grpc.proto.GetVariantBySkuResponse;
import com.hieu.catalog_service.interfaces.grpc.proto.Variant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Defensive wrapper around the catalog-service gRPC stub.
 * If catalog is unavailable the caller gets an empty Optional — never an exception —
 * so cart operations degrade gracefully (warnings added, but cart preserved).
 */
@Component
@RequiredArgsConstructor
public class CatalogGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogGrpcClient.class);
    private static final long DEADLINE_MS = 3000L;

    private final CatalogServiceGrpc.CatalogServiceBlockingStub stub;

    /**
     * Fetches a product with variants by id.
     *
     * @param productId catalog product id (UUID string)
     * @return response or empty on any failure
     */
    public Optional<GetProductResponse> getProduct(String productId) {
        try {
            var resp = stub.withDeadlineAfter(DEADLINE_MS, TimeUnit.MILLISECONDS)
                .getProduct(GetProductRequest.newBuilder().setProductId(productId).build());
            return Optional.of(resp);
        } catch (Exception e) {
            log.warn("catalog gRPC getProduct({}) failed: {}", productId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Fetches a variant by SKU — used during cart revalidation.
     *
     * @param sku variant SKU
     * @return response or empty on any failure
     */
    public Optional<GetVariantBySkuResponse> getVariantBySku(String sku) {
        try {
            var resp = stub.withDeadlineAfter(DEADLINE_MS, TimeUnit.MILLISECONDS)
                .getVariantBySku(GetVariantBySkuRequest.newBuilder().setSku(sku).build());
            return Optional.of(resp);
        } catch (Exception e) {
            log.warn("catalog gRPC getVariantBySku({}) failed: {}", sku, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Fetches a variant by id — resolves via product lookup then variant scan.
     * Returns empty if product not found or variant missing in response.
     */
    public Optional<Variant> getVariantById(String productId, String variantId) {
        return getProduct(productId)
            .filter(GetProductResponse::getFound)
            .map(GetProductResponse::getProduct)
            .flatMap(p -> p.getVariantsList().stream()
                .filter(v -> variantId.equals(v.getId()))
                .findFirst());
    }
}
