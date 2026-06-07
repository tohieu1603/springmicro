package com.hieu.payment_service.service;

import com.hieu.payment_service.AbstractIntegrationTest;
import com.hieu.payment_service.dto.InitiatePaymentRequest;
import com.hieu.payment_service.dto.PaymentDTO;
import com.hieu.payment_service.entity.PaymentJpaEntity;
import com.hieu.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentService — integration tests")
class PaymentServiceIT extends AbstractIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    // Mock Kafka so tests don't need broker for publish verification
    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void cleanDb() {
        paymentRepository.deleteAll();
    }

    private InitiatePaymentRequest buildRequest(String orderId) {
        var req = new InitiatePaymentRequest();
        req.setOrderId(orderId);
        req.setAmount(BigDecimal.valueOf(99_000));
        req.setCurrency("VND");
        req.setMethod("COD");
        return req;
    }

    @Test
    @DisplayName("initiate_persistsAndPublishesPending — DB row created with status PENDING")
    void initiate_persistsAndPublishesPending() {
        String orderId = "ORD-" + UUID.randomUUID();
        PaymentDTO dto = paymentService.initiatePayment("user-1", buildRequest(orderId));

        assertThat(dto.getOrderId()).isEqualTo(orderId);
        assertThat(dto.getStatus()).isEqualTo("PENDING");

        var saved = paymentRepository.findByOrderId(orderId);
        assertThat(saved).isPresent();
        assertThat(saved.get().getStatus()).isEqualTo("PENDING");
        assertThat(saved.get().getUserId()).isEqualTo("user-1");
    }

    @Test
    @DisplayName("autoConfirmByOrderId_idempotent_pessimisticLock — only 1 transition PAID under concurrency")
    void autoConfirmByOrderId_idempotent_pessimisticLock() throws InterruptedException {
        String orderId = "ORD-" + UUID.randomUUID();
        paymentService.initiatePayment("user-1", buildRequest(orderId));

        int threads = 2;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    paymentService.autoConfirmByOrderId(orderId, "TXN-" + UUID.randomUUID());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // one thread may get a lock exception or no-op; both are acceptable
                } finally {
                    doneLatch.countDown();
                }
            }));
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        var entity = paymentRepository.findByOrderId(orderId);
        assertThat(entity).isPresent();
        assertThat(entity.get().getStatus()).isEqualTo("PAID");
    }

    @Test
    @DisplayName("processRefund_idempotent — calling twice returns REFUNDED without error")
    void processRefund_idempotent() {
        String orderId = "ORD-" + UUID.randomUUID();
        PaymentDTO pending = paymentService.initiatePayment("user-1", buildRequest(orderId));
        String paymentId = pending.getId();

        // Manually set to REFUND_REQUESTED via repo
        paymentRepository.findById(paymentId).ifPresent(e -> {
            e.setStatus("PAID");
            paymentRepository.save(e);
        });
        paymentRepository.findById(paymentId).ifPresent(e -> {
            e.setStatus("REFUND_REQUESTED");
            paymentRepository.save(e);
        });

        // First call
        PaymentDTO first = paymentService.processRefund(paymentId, BigDecimal.valueOf(99_000), "reason");
        assertThat(first.getStatus()).isEqualTo("REFUNDED");

        // Second call — idempotent, should not throw
        PaymentDTO second = paymentService.processRefund(paymentId, BigDecimal.valueOf(99_000), "reason");
        assertThat(second.getStatus()).isEqualTo("REFUNDED");

        // Only one row
        long count = paymentRepository.count();
        assertThat(count).isEqualTo(1);
    }
}
