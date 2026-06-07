package com.hieu.analytics_service.kafka;

import com.hieu.analytics_service.document.AnalyticsEventDocument;
import com.hieu.analytics_service.service.AnalyticsIndexer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentEventConsumer — Unit")
class PaymentEventConsumerTest {

    @Mock
    private AnalyticsIndexer indexer;

    @Mock
    private Acknowledgment ack;

    @InjectMocks
    private PaymentEventConsumer consumer;

    @Test
    @DisplayName("indexes mapped document with referenceType=PAYMENT, paymentId as referenceId, amount parsed, acks")
    void happyPath_indexesWithPaymentReferenceTypeAndAcks() {
        consumer.onPaymentEvent(
                Map.of("paymentId", "pay-1", "amount", 1500, "currency", "VND", "status", "COMPLETED"),
                "payment.completed", ack);

        ArgumentCaptor<AnalyticsEventDocument> captor = ArgumentCaptor.forClass(AnalyticsEventDocument.class);
        verify(indexer).index(captor.capture());
        AnalyticsEventDocument doc = captor.getValue();
        assertThat(doc.getReferenceType()).isEqualTo("PAYMENT");
        assertThat(doc.getReferenceId()).isEqualTo("pay-1");
        assertThat(doc.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(doc.getCurrency()).isEqualTo("VND");
        assertThat(doc.getStatus()).isEqualTo("COMPLETED");
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("topic-derived fallback eventType: payment.failed -> PAYMENT_FAILED")
    void topic_derivesFallbackEventType() {
        consumer.onPaymentEvent(Map.of("paymentId", "pay-2"), "payment.failed", ack);

        ArgumentCaptor<AnalyticsEventDocument> captor = ArgumentCaptor.forClass(AnalyticsEventDocument.class);
        verify(indexer).index(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("PAYMENT_FAILED");
    }

    @Test
    @DisplayName("null topic uses PAYMENT_EVENT default fallback eventType")
    void nullTopic_usesDefaultFallback() {
        consumer.onPaymentEvent(Map.of("paymentId", "pay-3"), null, ack);

        ArgumentCaptor<AnalyticsEventDocument> captor = ArgumentCaptor.forClass(AnalyticsEventDocument.class);
        verify(indexer).index(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("PAYMENT_EVENT");
    }

    @Test
    @DisplayName("indexer failure propagates and offset is NOT acked")
    void indexFailure_doesNotAck() {
        doThrow(new RuntimeException("ES down")).when(indexer).index(any());

        assertThatThrownBy(() -> consumer.onPaymentEvent(Map.of("paymentId", "pay-4"), "payment.completed", ack))
                .isInstanceOf(RuntimeException.class);

        verify(ack, never()).acknowledge();
    }
}
