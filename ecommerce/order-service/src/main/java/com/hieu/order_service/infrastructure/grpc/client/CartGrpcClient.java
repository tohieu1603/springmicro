package com.hieu.order_service.infrastructure.grpc.client;

import com.hieu.cart_service.interfaces.grpc.proto.CartServiceGrpc;
import com.hieu.cart_service.interfaces.grpc.proto.ClearCartRequest;
import com.hieu.cart_service.interfaces.grpc.proto.GetCartRequest;
import com.hieu.order_service.domain.exception.EmptyCartException;
import com.hieu.order_service.domain.exception.ServiceUnavailableException;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * gRPC client for cart-service. Used by the "create order from cart" flow + the post-order
 * cart clear. {@link #getCartItems} throws {@link EmptyCartException} when the cart has no
 * items — the handler translates that into a 400 response. Transport failures surface as
 * {@link ServiceUnavailableException} so the saga can bail without a confusing "empty cart"
 * error when the real issue is network.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CartGrpcClient {

    private static final long DEADLINE_MS = 2000;

    private final CartServiceGrpc.CartServiceBlockingStub stub;

    private CartServiceGrpc.CartServiceBlockingStub deadlined() {
        return stub.withDeadlineAfter(DEADLINE_MS, TimeUnit.MILLISECONDS);
    }

    /** Fetch the user's cart items. Empty cart raises a business exception. */
    public List<CartItemSnapshot> getCartItems(String userId) {
        log.debug("gRPC getCart: userId={}", userId);
        try {
            var resp = deadlined().getCart(GetCartRequest.newBuilder().setUserId(userId).build());
            if (resp.getItemsCount() == 0) throw new EmptyCartException("Cart is empty for user " + userId);
            return resp.getItemsList().stream()
                    .map(i -> new CartItemSnapshot(
                            i.getProductId(), i.getProductName(), i.getVariantId(),  // now String
                            i.getVariantSku(), i.getVariantImage(), i.getUnitPrice(), i.getQuantity()))
                    .toList();
        } catch (EmptyCartException e) {
            throw e;
        } catch (StatusRuntimeException e) {
            log.error("gRPC getCart transport error: {} - {}", e.getStatus().getCode(), e.getMessage());
            throw new ServiceUnavailableException("cart-service");
        }
    }

    /** Fire-and-forget clear on the happy path — cart can be cleared manually if needed. */
    public void clearCart(String userId) {
        log.debug("gRPC clearCart: userId={}", userId);
        try {
            deadlined().clearCart(ClearCartRequest.newBuilder().setUserId(userId).build());
        } catch (StatusRuntimeException e) {
            log.warn("gRPC clearCart({}) failed: {} - {}", userId,
                    e.getStatus().getCode(), e.getMessage());
        }
    }

    /** Flat projection — saga converts these into OrderItems via the Order aggregate factory. */
    public record CartItemSnapshot(String productId, String productName, String variantId,
                                    String variantSku, String variantImage,
                                    String unitPrice, int quantity) {}
}
