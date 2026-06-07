package com.hieu.cart_service.grpc;

import com.hieu.cart_service.AbstractIntegrationTest;
import com.hieu.cart_service.dto.AddToCartRequest;
import com.hieu.cart_service.grpc.server.CartGrpcService;
import com.hieu.cart_service.interfaces.grpc.proto.GetCartRequest;
import com.hieu.cart_service.interfaces.grpc.proto.GetCartResponse;
import com.hieu.cart_service.repository.CartItemRepository;
import com.hieu.cart_service.service.CartService;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CartGrpcService — integration tests")
class CartGrpcServiceIT extends AbstractIntegrationTest {

    @Autowired CartGrpcService grpcService;
    @Autowired CartService cartService;
    @Autowired CartItemRepository cartItemRepository;

    private String userId;

    @AfterEach
    void cleanup() {
        if (userId != null) {
            cartItemRepository.deleteAllByUserId(userId);
        }
    }

    // ── getCart ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getCart_grpc_returnsItemsViaCacheLayer — gRPC delegate sang CartService (Redis cache + DB)")
    void getCart_grpc_returnsItemsViaCacheLayer() {
        userId = "grpc-user-" + UUID.randomUUID();

        // Seed cart via CartService
        cartService.addItem(userId, new AddToCartRequest("20", "500", 3, null));

        GetCartResponse resp = callGetCart(userId);

        assertThat(resp.getUserId()).isEqualTo(userId);
        assertThat(resp.getItemsList()).hasSize(1);
        assertThat(resp.getItemsList().get(0).getVariantId()).isEqualTo("500");
        assertThat(resp.getItemsList().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("getCart_grpc_emptyCart_returnsEmptyList — user chưa có cart → list rỗng")
    void getCart_grpc_emptyCart_returnsEmptyList() {
        userId = "grpc-user-empty-" + UUID.randomUUID();

        GetCartResponse resp = callGetCart(userId);

        assertThat(resp.getUserId()).isEqualTo(userId);
        assertThat(resp.getItemsList()).isEmpty();
    }

    // ── typed helpers ─────────────────────────────────────────────────────────

    private GetCartResponse callGetCart(String uid) {
        AtomicReference<GetCartResponse> holder = new AtomicReference<>();
        grpcService.getCart(
                GetCartRequest.newBuilder().setUserId(uid).build(),
                new StreamObserver<GetCartResponse>() {
                    @Override public void onNext(GetCartResponse v)      { holder.set(v); }
                    @Override public void onError(Throwable t)           { throw new AssertionError("gRPC error: " + t.getMessage(), t); }
                    @Override public void onCompleted()                  { /* no-op */ }
                });
        return holder.get();
    }
}
