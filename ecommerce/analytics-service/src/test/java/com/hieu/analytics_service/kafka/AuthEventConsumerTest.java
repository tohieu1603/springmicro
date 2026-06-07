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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthEventConsumer — Unit")
class AuthEventConsumerTest {

    @Mock
    private AnalyticsIndexer indexer;

    @Mock
    private Acknowledgment ack;

    @InjectMocks
    private AuthEventConsumer consumer;

    @Test
    @DisplayName("indexes mapped document with referenceType=USER and acks on success")
    void happyPath_indexesWithUserReferenceTypeAndAcks() {
        consumer.onAuthEvent(Map.of("userId", "u-9", "eventType", "USER_REGISTERED"),
                "auth.user-registered", ack);

        ArgumentCaptor<AnalyticsEventDocument> captor = ArgumentCaptor.forClass(AnalyticsEventDocument.class);
        verify(indexer).index(captor.capture());
        AnalyticsEventDocument doc = captor.getValue();
        assertThat(doc.getReferenceType()).isEqualTo("USER");
        assertThat(doc.getUserId()).isEqualTo("u-9");
        assertThat(doc.getEventType()).isEqualTo("USER_REGISTERED");
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("topic-derived fallback eventType: auth.user.events.v1 -> AUTH_USER_EVENTS_V1")
    void topic_derivesFallbackEventType() {
        consumer.onAuthEvent(Map.of("userId", "u-10"), "auth.user.events.v1", ack);

        ArgumentCaptor<AnalyticsEventDocument> captor = ArgumentCaptor.forClass(AnalyticsEventDocument.class);
        verify(indexer).index(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("AUTH_USER_EVENTS_V1");
    }

    @Test
    @DisplayName("null topic uses AUTH_EVENT default fallback eventType")
    void nullTopic_usesDefaultFallback() {
        consumer.onAuthEvent(Map.of("userId", "u-11"), null, ack);

        ArgumentCaptor<AnalyticsEventDocument> captor = ArgumentCaptor.forClass(AnalyticsEventDocument.class);
        verify(indexer).index(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("AUTH_EVENT");
    }

    @Test
    @DisplayName("indexer failure propagates and offset is NOT acked")
    void indexFailure_doesNotAck() {
        doThrow(new RuntimeException("ES down")).when(indexer).index(any());

        assertThatThrownBy(() -> consumer.onAuthEvent(Map.of("userId", "u-12"), "auth.user-registered", ack))
                .isInstanceOf(RuntimeException.class);

        verify(ack, never()).acknowledge();
    }
}
