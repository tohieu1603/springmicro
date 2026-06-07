package com.hieu.shipping_service.kafka;

import com.hieu.shipping_service.AbstractIntegrationTest;
import com.hieu.shipping_service.repository.ShipmentRepository;
import com.hieu.shipping_service.service.OrderServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Duration;

@DisplayName("PaymentCompletedConsumer — Kafka integration tests")
class PaymentCompletedConsumerIT extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @MockitoBean
    private OrderServiceClient orderServiceClient;

    private static final Map<String, Object> SHIPPING_ADDRESS = Map.of(
            "recipientName", "Nguyen Van A",
            "recipientPhone", "0901234567",
            "addressLine", "123 Le Loi",
            "ward", "Phuong 1",
            "district", "Quan 1",
            "city", "Ho Chi Minh",
            "country", "Vietnam"
    );

    @BeforeEach
    void setUp() {
        shipmentRepository.deleteAll();
        when(orderServiceClient.fetchOrder(anyString()))
                .thenReturn(Optional.of(Map.of("shippingAddress", SHIPPING_ADDRESS)));
    }

    @Test
    @DisplayName("paymentCompleted_createsShipment — after publishing event, shipment row exists")
    void paymentCompleted_createsShipment() {
        String orderId = "ORD-" + UUID.randomUUID();
        String userId = "user-kafka-1";

        var event = new PaymentCompletedEvent(
                UUID.randomUUID().toString(),
                orderId,
                userId,
                "PAID"
        );

        kafkaTemplate.send(ShippingTopics.PAYMENT_COMPLETED, orderId, event);

        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() ->
                        assertThat(shipmentRepository.findByOrderId(orderId))
                                .isPresent()
                );
    }

    @Test
    @DisplayName("paymentCompleted_duplicateEvent_idempotent — 2 events same orderId → 1 shipment")
    void paymentCompleted_duplicateEvent_idempotent() throws InterruptedException {
        String orderId = "ORD-" + UUID.randomUUID();
        String userId = "user-kafka-2";

        var event = new PaymentCompletedEvent(
                UUID.randomUUID().toString(),
                orderId,
                userId,
                "PAID"
        );

        kafkaTemplate.send(ShippingTopics.PAYMENT_COMPLETED, orderId, event);
        kafkaTemplate.send(ShippingTopics.PAYMENT_COMPLETED, orderId, event);

        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() -> {
                    long count = shipmentRepository.count();
                    assertThat(count).isEqualTo(1);
                    assertThat(shipmentRepository.findByOrderId(orderId)).isPresent();
                });
    }
}
