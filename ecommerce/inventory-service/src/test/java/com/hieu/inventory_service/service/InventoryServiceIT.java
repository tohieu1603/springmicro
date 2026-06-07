package com.hieu.inventory_service.service;

import com.hieu.inventory_service.AbstractIntegrationTest;
import com.hieu.inventory_service.dto.ReservationRequest;
import com.hieu.inventory_service.exception.InsufficientStockException;
import com.hieu.inventory_service.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InventoryService — integration tests")
class InventoryServiceIT extends AbstractIntegrationTest {

    @Autowired InventoryService inventoryService;
    @Autowired InventoryRepository inventoryRepository;
    @Autowired StockRedisService stockRedisService;

    private String productId;
    private String sku;

    @BeforeEach
    void seedInventory() {
        productId = UUID.randomUUID().toString();
        sku = "SKU-IT-" + productId;
        // Ensure no duplicate productId (idempotent clean)
        inventoryRepository.findByProductId(productId)
                .ifPresent(e -> inventoryRepository.delete(e));
        inventoryService.create(productId, sku, 100, 10);
    }

    // ── reserveStock — happy path ─────────────────────────────────────────────

    @Test
    @DisplayName("reserveStock_happyPath_decrementsRedisAndDb — dự trữ thành công")
    void reserveStock_happyPath_decrementsRedisAndDb() {
        String orderId = "ORDER-" + UUID.randomUUID();

        var result = inventoryService.reserveStock(new ReservationRequest(
                orderId,
                List.of(new ReservationRequest.ReservationItem(productId, 10))
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.reservationId()).isNotBlank();

        var entity = inventoryRepository.findByProductId(productId).orElseThrow();
        assertThat(entity.getReservedQuantity()).isEqualTo(10);
        assertThat(entity.getAvailableQuantity()).isEqualTo(90);
    }

    // ── reserveStock — insufficient Redis ────────────────────────────────────

    @Test
    @DisplayName("reserveStock_insufficientRedis_throwsBeforeDb — Redis không đủ → InsufficientStockException")
    void reserveStock_insufficientRedis_throwsBeforeDb() {
        // Seed Redis with very low stock (1), DB still has 100
        stockRedisService.setStock(productId, 1);
        String orderId = "ORDER-INSUF-" + UUID.randomUUID();

        assertThatThrownBy(() -> inventoryService.reserveStock(new ReservationRequest(
                orderId,
                List.of(new ReservationRequest.ReservationItem(productId, 50))
        ))).isInstanceOf(InsufficientStockException.class);

        // DB should remain unchanged
        var entity = inventoryRepository.findByProductId(productId).orElseThrow();
        assertThat(entity.getReservedQuantity()).isZero();
    }

    // ── reserveStock — idempotency ────────────────────────────────────────────

    @Test
    @DisplayName("reserveStock_idempotency — cùng orderId gọi 2 lần → kết quả như nhau")
    void reserveStock_idempotent_secondCallReturnsSameResult() {
        String orderId = "ORDER-IDEM-" + UUID.randomUUID();
        var items = List.of(new ReservationRequest.ReservationItem(productId, 5));

        var r1 = inventoryService.reserveStock(new ReservationRequest(orderId, items));
        var r2 = inventoryService.reserveStock(new ReservationRequest(orderId, items));

        assertThat(r1.success()).isTrue();
        assertThat(r2.success()).isTrue();

        // DB reserved should only be 5, not 10
        var entity = inventoryRepository.findByProductId(productId).orElseThrow();
        assertThat(entity.getReservedQuantity()).isEqualTo(5);
    }

    // ── reserveStock — concurrent ─────────────────────────────────────────────

    @Test
    @DisplayName("reserveStock_concurrentSameItems_pessimisticLockSerializes — 5 threads → final reserved đúng, no deadlock")
    void reserveStock_concurrentSameItems_pessimisticLockSerializes() throws InterruptedException {
        int threads = 5;
        int qtyPerThread = 5; // total = 25, within available 100
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            final String orderId = "ORDER-CONC-" + i + "-" + UUID.randomUUID();
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    inventoryService.reserveStock(new ReservationRequest(
                            orderId,
                            List.of(new ReservationRequest.ReservationItem(productId, qtyPerThread))
                    ));
                    successes.incrementAndGet();
                } catch (Exception e) {
                    failures.incrementAndGet();
                }
            });
        }

        ready.await();
        start.countDown();
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);

        var entity = inventoryRepository.findByProductId(productId).orElseThrow();
        int expectedReserved = successes.get() * qtyPerThread;

        assertThat(entity.getReservedQuantity())
                .as("Reserved phải bằng successes * qty — không có dirty write")
                .isEqualTo(expectedReserved);
        assertThat(successes.get())
                .as("Ít nhất 1 thread thành công")
                .isGreaterThanOrEqualTo(1);
    }
}
