package com.hieu.analytics_service.service;

import com.hieu.analytics_service.document.AnalyticsEventDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Writes analytics events into a date-partitioned ES index: {prefix}-YYYY.MM.dd.
 * Index is auto-created on first write so Kibana data view can use {prefix}-* pattern.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsIndexer {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneOffset.UTC);

    private final ElasticsearchTemplate esTemplate;

    @Value("${analytics.index-prefix:analytics-events}")
    private String indexPrefix;

    public void index(AnalyticsEventDocument event) {
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }
        String indexName = resolveIndex(event.getTimestamp());
        ensureIndex(indexName);
        IndexCoordinates coords = IndexCoordinates.of(indexName);
        esTemplate.save(event, coords);
        log.debug("Indexed event type={} userId={} into {}",
                event.getEventType(), event.getUserId(), indexName);
    }

    private String resolveIndex(Instant ts) {
        return indexPrefix + "-" + DATE_FMT.format(ts);
    }

    /**
     * Lazily create the daily index with a minimal mapping so Kibana shows fields correctly.
     * Single atomic create(settings, mapping) call eliminates TOCTOU race between exists()
     * check and create(); concurrent pods both attempt create — the loser's exception is swallowed.
     */
    private void ensureIndex(String indexName) {
        IndexCoordinates coords = IndexCoordinates.of(indexName);
        IndexOperations ops = esTemplate.indexOps(coords);
        if (ops.exists()) return;
        Document mapping = Document.parse("""
                { "properties": {
                    "eventType":     { "type": "keyword" },
                    "userId":        { "type": "keyword" },
                    "referenceId":   { "type": "keyword" },
                    "referenceType": { "type": "keyword" },
                    "amount":        { "type": "scaled_float", "scaling_factor": 100 },
                    "currency":      { "type": "keyword" },
                    "status":        { "type": "keyword" },
                    "timestamp":     { "type": "date" },
                    "metadata":      { "type": "object", "enabled": true }
                }}""");
        try {
            // Atomic: create + apply mapping in one ES call. Concurrent pods racing here both
            // throw on the loser; catch swallows since the result is identical.
            ops.create(java.util.Map.of(), mapping);
            log.info("Created analytics index {}", indexName);
        } catch (Exception e) {
            log.debug("Index {} already exists or concurrent create: {}", indexName, e.getMessage());
        }
    }
}
