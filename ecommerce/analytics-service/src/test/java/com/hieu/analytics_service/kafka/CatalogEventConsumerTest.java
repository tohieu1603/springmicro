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
@DisplayName("CatalogEventConsumer — Unit")
class CatalogEventConsumerTest {

    @Mock
    private AnalyticsIndexer indexer;

    @Mock
    private Acknowledgment ack;

    @InjectMocks
    private CatalogEventConsumer consumer;

    @Test
    @DisplayName("indexes mapped document with referenceType=PRODUCT and resolves productId as referenceId")
    void happyPath_indexesWithProductReferenceTypeAndAcks() {
        consumer.onCatalogEvent(Map.of("productId", "p-1"), "catalog.product-created", ack);

        ArgumentCaptor<AnalyticsEventDocument> captor = ArgumentCaptor.forClass(AnalyticsEventDocument.class);
        verify(indexer).index(captor.capture());
        AnalyticsEventDocument doc = captor.getValue();
        assertThat(doc.getReferenceType()).isEqualTo("PRODUCT");
        assertThat(doc.getReferenceId()).isEqualTo("p-1");
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("topic-derived fallback eventType: catalog.variant-price-changed -> CATALOG_VARIANT-PRICE-CHANGED")
    void topic_derivesFallbackEventType_dotsToUnderscoresHyphensKept() {
        consumer.onCatalogEvent(Map.of("productId", "p-2"), "catalog.variant-price-changed", ack);

        ArgumentCaptor<AnalyticsEventDocument> captor = ArgumentCaptor.forClass(AnalyticsEventDocument.class);
        verify(indexer).index(captor.capture());
        // Only '.' is replaced with '_'; hyphens are preserved by the consumer's transform.
        assertThat(captor.getValue().getEventType()).isEqualTo("CATALOG_VARIANT-PRICE-CHANGED");
    }

    @Test
    @DisplayName("null topic uses CATALOG_EVENT default fallback eventType")
    void nullTopic_usesDefaultFallback() {
        consumer.onCatalogEvent(Map.of("productId", "p-3"), null, ack);

        ArgumentCaptor<AnalyticsEventDocument> captor = ArgumentCaptor.forClass(AnalyticsEventDocument.class);
        verify(indexer).index(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("CATALOG_EVENT");
    }

    @Test
    @DisplayName("indexer failure propagates and offset is NOT acked")
    void indexFailure_doesNotAck() {
        doThrow(new RuntimeException("ES down")).when(indexer).index(any());

        assertThatThrownBy(() -> consumer.onCatalogEvent(Map.of("productId", "p-4"), "catalog.product-created", ack))
                .isInstanceOf(RuntimeException.class);

        verify(ack, never()).acknowledge();
    }
}
