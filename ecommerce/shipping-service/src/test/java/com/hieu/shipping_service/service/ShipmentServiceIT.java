package com.hieu.shipping_service.service;

import com.hieu.shipping_service.AbstractIntegrationTest;
import com.hieu.shipping_service.dto.CreateShipmentRequest;
import com.hieu.shipping_service.exception.InvalidShipmentStateException;
import com.hieu.shipping_service.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ShipmentService — integration tests")
class ShipmentServiceIT extends AbstractIntegrationTest {

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void cleanDb() {
        shipmentRepository.deleteAll();
    }

    private CreateShipmentRequest buildRequest(String orderId) {
        return new CreateShipmentRequest(
                orderId,
                "user-1",
                null,
                "Nguyen Van A",
                "0901234567",
                "123 Le Loi",
                "Phuong 1",
                "Quan 1",
                "Ho Chi Minh",
                "Vietnam",
                null
        );
    }

    @Test
    @DisplayName("createShipmentIfAbsent_idempotent — calling twice with same orderId creates 1 record")
    void createShipmentIfAbsent_idempotent() {
        String orderId = "ORD-" + UUID.randomUUID();
        shipmentService.createShipmentIfAbsent(buildRequest(orderId));
        shipmentService.createShipmentIfAbsent(buildRequest(orderId)); // must not throw

        long count = shipmentRepository.count();
        assertThat(count).isEqualTo(1);
        assertThat(shipmentRepository.findByOrderId(orderId)).isPresent();
    }

    @Test
    @DisplayName("createShipmentIfAbsent_concurrentInsert_handlesGracefully — 5 threads, final 1 row")
    void createShipmentIfAbsent_concurrentInsert_handlesGracefully() throws InterruptedException {
        String orderId = "ORD-" + UUID.randomUUID();
        int threads = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    shipmentService.createShipmentIfAbsent(buildRequest(orderId));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }));
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // Final state: exactly 1 row regardless of concurrency
        long count = shipmentRepository.count();
        assertThat(count).isEqualTo(1);
        assertThat(shipmentRepository.findByOrderId(orderId)).isPresent();
    }

    @Test
    @DisplayName("updateStatus_invalidTransition_throws — PENDING → DELIVERED throws")
    void updateStatus_invalidTransition_throws() {
        String orderId = "ORD-" + UUID.randomUUID();
        shipmentService.createShipment(buildRequest(orderId));
        var entity = shipmentRepository.findByOrderId(orderId).orElseThrow();

        assertThatThrownBy(() ->
                shipmentService.updateStatus(entity.getId(), "DELIVERED", null)
        ).isInstanceOf(InvalidShipmentStateException.class)
                .hasMessageContaining("Cannot transition from PENDING to DELIVERED");
    }
}
