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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventConsumer — Unit")
class OrderEventConsumerTest {

    @Mock
    private AnalyticsIndexer indexer;

    @Mock
    private Acknowledgment ack;

    @InjectMocks
    private OrderEventConsumer consumer;

    @Test
    @DisplayName("indexes mapped document with referenceType=ORDER and acks on success")
    void happyPath_indexesAndAcks() {
        Map<String, Object> payload = Map.of("orderId", "o-1", "userId", "u-1");

        consumer.onOrderEvent(payload, "order.placed", ack);

        ArgumentCaptor<AnalyticsEventDocument> captor = ArgumentCaptor.forClass(AnalyticsEventDocument.class);
        verify(indexer).index(captor.capture());
        AnalyticsEventDocument doc = captor.getValue();
        assertThat(doc.getReferenceType()).isEqualTo("ORDER");
        assertThat(doc.getReferenceId()).isEqualTo("o-1");
        assertThat(doc.getUserId()).isEqualTo("u-1");
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("topic name becomes fallback eventType: lowercased dots -> UPPER underscores")
    void topic_derivesFallbackEventType() {
        consumer.onOrderEvent(Map.of("orderId", "o-2"), "order.cancelled", ack);

        ArgumentCaptor<AnalyticsEventDocument> captor = ArgumentCaptor.forClass(AnalyticsEventDocument.class);
        verify(indexer).index(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("ORDER_CANCELLED");
    }

    @Test
    @DisplayName("payload eventType wins over topic-derived fallback")
    void payloadEventType_overridesFallback() {
        consumer.onOrderEvent(Map.of("eventType", "CUSTOM_TYPE", "orderId", "o-3"), "order.placed", ack);

        ArgumentCaptor<AnalyticsEventDocument> captor = ArgumentCaptor.forClass(AnalyticsEventDocument.class);
        verify(indexer).index(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("CUSTOM_TYPE");
    }

    @Test
    @DisplayName("null topic uses ORDER_EVENT default fallback eventType")
    void nullTopic_usesDefaultFallback() {
        consumer.onOrderEvent(Map.of("orderId", "o-4"), null, ack);

        ArgumentCaptor<AnalyticsEventDocument> captor = ArgumentCaptor.forClass(AnalyticsEventDocument.class);
        verify(indexer).index(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("ORDER_EVENT");
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("indexer failure propagates and offset is NOT acked")
    void indexFailure_doesNotAck() {
        doThrow(new RuntimeException("ES down")).when(indexer).index(org.mockito.ArgumentMatchers.any());

        assertThatThrownBy(() -> consumer.onOrderEvent(Map.of("orderId", "o-5"), "order.placed", ack))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("ES down");

        verify(ack, never()).acknowledge();
    }
}
