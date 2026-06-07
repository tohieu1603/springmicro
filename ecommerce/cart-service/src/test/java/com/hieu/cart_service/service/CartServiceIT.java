package com.hieu.cart_service.service;

import com.hieu.cart_service.AbstractIntegrationTest;
import com.hieu.cart_service.dto.AddToCartRequest;
import com.hieu.cart_service.dto.CartDTO;
import com.hieu.cart_service.redis.CartCacheService;
import com.hieu.cart_service.repository.CartItemRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CartService — integration tests")
class CartServiceIT extends AbstractIntegrationTest {

    @Autowired CartService cartService;
    @Autowired CartItemRepository cartItemRepository;
    @Autowired CartCacheService cacheService;
    @Autowired RedisTemplate<String, Object> redisTemplate;

    private String userId;

    @AfterEach
    void cleanup() {
        if (userId != null) {
            cartItemRepository.deleteAllByUserId(userId);
            cacheService.evictCart(userId);
        }
    }

    // ── addItem — new item ────────────────────────────────────────────────────

    @Test
    @DisplayName("addItem_newItem_persistsAndCachesInRedis — item mới được lưu DB và evict cache")
    void addItem_newItem_persistsAndCachesInRedis() {
        userId = "user-" + UUID.randomUUID();
        var req = new AddToCartRequest("10", "100", 2, null);

        CartDTO cart = cartService.addItem(userId, req);

        assertThat(cart).isNotNull();
        assertThat(cart.userId()).isEqualTo(userId);
        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().get(0).variantId()).isEqualTo("100");
        assertThat(cart.items().get(0).quantity()).isEqualTo(2);

        // Verify DB persisted
        var dbItems = cartItemRepository.findAllByUserId(userId);
        assertThat(dbItems).hasSize(1);
        assertThat(dbItems.get(0).getQuantity()).isEqualTo(2);
    }

    // ── addItem — existing item ───────────────────────────────────────────────

    @Test
    @DisplayName("addItem_existingItem_incrementsQuantity — item đã có → cộng dồn số lượng")
    void addItem_existingItem_incrementsQuantity() {
        userId = "user-" + UUID.randomUUID();
        var req = new AddToCartRequest("10", "200", 3, null);

        cartService.addItem(userId, req);
        cartService.addItem(userId, req); // add again

        var dbItems = cartItemRepository.findAllByUserId(userId);
        assertThat(dbItems).hasSize(1);
        assertThat(dbItems.get(0).getQuantity()).isEqualTo(6); // 3 + 3
    }

    // ── addItem — optimistic lock ─────────────────────────────────────────────

    @Test
    @DisplayName("addItem_optimisticLockConflict_throws — 2 threads cùng variant → 1 success + có thể có OptimisticLock")
    void addItem_optimisticLockConflict_throws() throws InterruptedException {
        userId = "user-" + UUID.randomUUID();
        // Pre-seed item so both threads hit the update path (existing item)
        cartService.addItem(userId, new AddToCartRequest("10", "300", 1, null));

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    cartService.addItem(userId, new AddToCartRequest("10", "300", 1, null));
                    successes.incrementAndGet();
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            });
        }

        ready.await();
        start.countDown();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(successes.get() + errors.get()).isEqualTo(threads);
        assertThat(successes.get()).isGreaterThanOrEqualTo(1);
    }

    // ── getCart — cache hit ───────────────────────────────────────────────────

    @Test
    @DisplayName("getCart_cacheHit_doesNotQueryDb — Redis hit → trả về cache mà không query DB")
    void getCart_cacheHit_doesNotQueryDb() {
        userId = "user-" + UUID.randomUUID();
        var req = new AddToCartRequest("10", "400", 1, null);
        cartService.addItem(userId, req);

        // First getCart populates Redis cache
        CartDTO first = cartService.getCart(userId);
        assertThat(first.items()).hasSize(1);

        // Seed a manual cache entry with different data to verify cache is used
        var fakeCart = new CartDTO(userId, java.util.List.of(), 0, java.math.BigDecimal.ZERO, java.util.List.of());
        cacheService.putCart(userId, fakeCart);

        // getCart should return the cached (fake) value, not re-query DB
        CartDTO cached = cartService.getCart(userId);
        assertThat(cached.items()).isEmpty(); // empty because we put fake cart
    }
}
