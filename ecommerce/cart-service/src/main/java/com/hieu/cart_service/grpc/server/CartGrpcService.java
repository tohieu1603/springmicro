package com.hieu.cart_service.grpc.server;

import com.hieu.cart_service.interfaces.grpc.proto.CartItemSnapshot;
import com.hieu.cart_service.interfaces.grpc.proto.CartServiceGrpc;
import com.hieu.cart_service.interfaces.grpc.proto.ClearCartRequest;
import com.hieu.cart_service.interfaces.grpc.proto.ClearCartResponse;
import com.hieu.cart_service.interfaces.grpc.proto.GetCartRequest;
import com.hieu.cart_service.interfaces.grpc.proto.GetCartResponse;
import com.hieu.cart_service.service.CartService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;

/**
 * gRPC server implementation for CartService.
 * Used by order-service to read/clear a user's cart before placing an order.
 */
@GrpcService
@RequiredArgsConstructor
public class CartGrpcService extends CartServiceGrpc.CartServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(CartGrpcService.class);

    private final CartService cartService;

    @Override
    public void getCart(GetCartRequest request, StreamObserver<GetCartResponse> observer) {
        // Validate userId is present — prevents empty-key lookups that would
        // return all items with userId="" or cause NPE in downstream repos.
        // NOTE: this port must be gated by service-mesh mTLS / network policy;
        // gRPC calls do not pass through the servlet JWT filter.
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            observer.onError(io.grpc.Status.INVALID_ARGUMENT
                    .withDescription("userId must not be blank")
                    .asRuntimeException());
            return;
        }
        try {
            // Modernization: delegate to CartService to hit Redis cache + catalog revalidation.
            var cartDTO = cartService.getCart(request.getUserId());
            var snapshots = cartDTO.items().stream().map(item -> CartItemSnapshot.newBuilder()
                    .setId(item.id() != null ? item.id() : "")
                    .setUserId(request.getUserId())
                    .setProductId(item.productId() != null ? item.productId() : "")
                    .setProductName(nz(item.productName()))
                    .setVariantId(item.variantId() != null ? item.variantId() : "")
                    .setVariantSku(nz(item.variantSku()))
                    .setVariantImage(nz(item.variantImage()))
                    .setUnitPrice(item.unitPrice() != null ? item.unitPrice().toPlainString() : "0")
                    .setQuantity(item.quantity() != null ? item.quantity() : 0)
                    .build()).toList();
            var reply = GetCartResponse.newBuilder()
                .setUserId(request.getUserId())
                .addAllItems(snapshots)
                .build();
            observer.onNext(reply);
        } catch (Exception e) {
            // Propagate as gRPC INTERNAL so callers (order-service saga) don't mistake
            // an empty response for an empty cart and silently create an order with no
            // items. Returning empty here masked real failures (Redis/DB errors).
            log.error("gRPC getCart failed for user {}: {}", request.getUserId(), e.getMessage(), e);
            observer.onError(io.grpc.Status.INTERNAL
                    .withDescription("getCart failed: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
            return;
        }
        observer.onCompleted();
    }

    @Override
    public void clearCart(ClearCartRequest request, StreamObserver<ClearCartResponse> observer) {
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            observer.onError(io.grpc.Status.INVALID_ARGUMENT
                    .withDescription("userId must not be blank")
                    .asRuntimeException());
            return;
        }
        try {
            cartService.clearCart(request.getUserId());
            observer.onNext(ClearCartResponse.newBuilder().setSuccess(true).build());
        } catch (Exception e) {
            log.error("gRPC clearCart failed for user {}: {}", request.getUserId(), e.getMessage(), e);
            observer.onError(io.grpc.Status.INTERNAL
                    .withDescription("clearCart failed: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
            return;
        }
        observer.onCompleted();
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
