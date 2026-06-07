package com.hieu.cart_service.service;

import com.hieu.cart_service.dto.AddToCartRequest;
import com.hieu.cart_service.dto.CartDTO;
import com.hieu.cart_service.dto.UpdateCartItemRequest;
import com.hieu.cart_service.entity.CartItem;
import com.hieu.cart_service.grpc.client.CatalogGrpcClient;
import com.hieu.cart_service.redis.CartCacheService;
import com.hieu.cart_service.repository.CartItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for the catalog-gRPC-free paths of {@link CartService}: cache hit/miss,
 * line-subtotal + grand-total computation, idempotency replay, remove/clear, and the
 * catalog-unavailable guard. The catalog round-trip (validate + add) needs gRPC protos and
 * is covered by the integration tests.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CartService (unit)")
class CartServiceTest {

    @Mock CartItemRepository cartItemRepository;
    @Mock CartCacheService cacheService;

    CartService service;

    @BeforeEach
    void setup() {
        // Empty catalog client → the gRPC validate path short-circuits; the paths under
        // test here never need it (or assert the SERVICE_UNAVAILABLE guard).
        service = new CartService(cartItemRepository, cacheService, Optional.<CatalogGrpcClient>empty());
    }

    private static CartItem item(String id, String variantId, BigDecimal unitPrice, int qty) {
        return CartItem.builder()
                .id(id).userId("u1").productId("10").productName("Product")
                .variantId(variantId).variantSku("SKU-" + variantId)
                .unitPrice(unitPrice).quantity(qty)
                .build();
    }

    @Test
    @DisplayName("getCart returns the cached cart without hitting the database")
    void getCart_cacheHit() {
        CartDTO cached = new CartDTO("u1", List.of(), 0, BigDecimal.ZERO, List.of());
        when(cacheService.getCart("u1")).thenReturn(cached);

        assertThat(service.getCart("u1")).isSameAs(cached);
        verifyNoInteractions(cartItemRepository);
    }

    @Test
    @DisplayName("getCart on a cache miss builds the cart and computes the grand total")
    void getCart_cacheMiss_computesTotals() {
        when(cacheService.getCart("u1")).thenReturn(null);
        when(cartItemRepository.findAllByUserId("u1"))
                .thenReturn(List.of(item("1", "100", BigDecimal.valueOf(50_000), 2)));

        CartDTO cart = service.getCart("u1");

        assertThat(cart.totalItems()).isEqualTo(1);
        assertThat(cart.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
        verify(cacheService).putCart(eq("u1"), any(CartDTO.class));
    }

    @Test
    @DisplayName("removeItem deletes the line, evicts the cache and recomputes totals")
    void removeItem() {
        when(cartItemRepository.findAllByUserId("u1"))
                .thenReturn(List.of(item("2", "200", BigDecimal.valueOf(30_000), 1)));

        CartDTO cart = service.removeItem("u1", "100");

        assertThat(cart.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(30_000));
        verify(cartItemRepository).deleteByUserIdAndVariantId("u1", "100");
        verify(cacheService).evictCart("u1");
    }

    @Test
    @DisplayName("clearCart deletes all items and evicts the cache")
    void clearCart() {
        service.clearCart("u1");
        verify(cartItemRepository).deleteAllByUserId("u1");
        verify(cacheService).evictCart("u1");
    }

    @Test
    @DisplayName("addItem replays the cached result for a known idempotency key")
    void addItem_idempotentReplay() {
        CartDTO cached = new CartDTO("u1", List.of(), 0, BigDecimal.ZERO, List.of());
        when(cacheService.getIdempotentResult("k1")).thenReturn(cached);

        CartDTO result = service.addItem("u1", new AddToCartRequest("10", "100", 1, "k1"));

        assertThat(result).isSameAs(cached);
        verifyNoInteractions(cartItemRepository);
    }

    @Test
    @DisplayName("addItem fails with 503 when the catalog client is unavailable")
    void addItem_catalogUnavailable() {
        assertThatThrownBy(() -> service.addItem("u1", new AddToCartRequest("10", "100", 1, null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(503));
    }

    @Test
    @DisplayName("updateItem with quantity 0 deletes the item")
    void updateItem_zeroDeletes() {
        when(cartItemRepository.findAllByUserId("u1")).thenReturn(List.of());

        service.updateItem("u1", "100", new UpdateCartItemRequest(0));

        verify(cartItemRepository).deleteByUserIdAndVariantId("u1", "100");
    }

    @Test
    @DisplayName("removeItemsByProduct deletes by product and evicts every affected user")
    void removeItemsByProduct() {
        when(cartItemRepository.findUserIdsByProductId("10")).thenReturn(List.of("u1", "u2"));

        service.removeItemsByProduct("10");

        verify(cartItemRepository).deleteAllByProductId("10");
        verify(cacheService).evictCart("u1");
        verify(cacheService).evictCart("u2");
    }
}
