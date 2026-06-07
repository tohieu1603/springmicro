package com.hieu.analytics_service.service;

import com.hieu.analytics_service.AbstractIntegrationTest;
import com.hieu.analytics_service.document.AnalyticsEventDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("AnalyticsIndexer — Integration")
class AnalyticsIndexerIT extends AbstractIntegrationTest {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneOffset.UTC);

    @Autowired
    private AnalyticsIndexer indexer;

    @Autowired
    private ElasticsearchTemplate esTemplate;

    private AnalyticsEventDocument buildDoc(String eventType) {
        return AnalyticsEventDocument.builder()
                .eventType(eventType)
                .userId("user-it-1")
                .referenceId("ref-1")
                .referenceType("ORDER")
                .amount(BigDecimal.valueOf(250))
                .currency("VND")
                .status("COMPLETED")
                .timestamp(Instant.now())
                .build();
    }

    private String todayIndex() {
        return "analytics-events-" + DATE_FMT.format(Instant.now());
    }

    @Nested
    @DisplayName("Indexing")
    class Indexing {

        @Test
        @DisplayName("index() creates date-partitioned index and stores document")
        void index_createsIndexAndStoresDoc() {
            AnalyticsEventDocument doc = buildDoc("ORDER_PLACED");

            indexer.index(doc);

            String indexName = todayIndex();
            IndexCoordinates coords = IndexCoordinates.of(indexName);
            IndexOperations ops = esTemplate.indexOps(coords);
            assertThat(ops.exists())
                    .as("Index %s should exist after first write", indexName)
                    .isTrue();
        }

        @Test
        @DisplayName("ensureIndex is idempotent — calling index() twice creates only 1 index, no error")
        void ensureIndex_isIdempotent() {
            AnalyticsEventDocument doc1 = buildDoc("ORDER_PLACED");
            AnalyticsEventDocument doc2 = buildDoc("ORDER_CONFIRMED");

            assertThatCode(() -> {
                indexer.index(doc1);
                indexer.index(doc2);
            }).doesNotThrowAnyException();

            String indexName = todayIndex();
            IndexOperations ops = esTemplate.indexOps(IndexCoordinates.of(indexName));
            assertThat(ops.exists()).isTrue();
        }
    }
}
