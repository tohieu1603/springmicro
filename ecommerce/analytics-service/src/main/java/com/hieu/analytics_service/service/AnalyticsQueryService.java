package com.hieu.analytics_service.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.hieu.analytics_service.document.AnalyticsEventDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight aggregations against analytics-events-* indices for sanity-check endpoints.
 * Kibana is the main analytics UI — this exists for scripts / health probes only.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsQueryService {

    private static final String FIELD_TIMESTAMP = "timestamp";
    private static final String FIELD_EVENT_TYPE = "eventType";


    private final ElasticsearchTemplate esTemplate;

    @Value("${analytics.index-prefix:analytics-events}")
    private String indexPrefix;

    public Map<String, Object> summary(Instant from, Instant to) {
        IndexCoordinates indices = IndexCoordinates.of(indexPrefix + "-*");
        Map<String, Object> result = new HashMap<>();
        result.put("from", from);
        result.put("to", to);

        try {
            result.put("totalEvents", countByEventType(indices, from, to, null));
            result.put("orderPlaced", countByEventType(indices, from, to, "ORDER_PLACED"));
            result.put("orderCancelled", countByEventType(indices, from, to, "ORDER_CANCELLED"));
            result.put("paymentCompleted", countByEventType(indices, from, to, "PAYMENT_COMPLETED"));
            result.put("paymentFailed", countByEventType(indices, from, to, "PAYMENT_FAILED"));
            result.put("usersRegistered", countByEventType(indices, from, to, "USER_REGISTERED"));
        } catch (Exception e) {
            log.warn("Summary query failed: {}", e.getMessage());
            result.put("error", e.getMessage());
        }
        return result;
    }

    private long countByEventType(IndexCoordinates indices, Instant from, Instant to, String eventType) {
        var q = NativeQuery.builder()
                .withQuery(buildRangeAndType(from, to, eventType))
                .withMaxResults(0)
                .build();
        SearchHits<Map> hits = esTemplate.search(q, Map.class, indices);
        return hits.getTotalHits();
    }

    private Query buildRangeAndType(Instant from, Instant to, String eventType) {
        return Query.of(qb -> qb.bool(b -> {
            b.must(m -> m.range(r -> r.date(d -> d
                    .field(FIELD_TIMESTAMP)
                    .gte(from.toString())
                    .lt(to.toString()))));
            if (eventType != null) {
                b.must(m -> m.term(t -> t.field(FIELD_EVENT_TYPE).value(eventType)));
            }
            return b;
        }));
    }

    /**
     * Returns the last {@code size} log-like analytics events optionally filtered
     * by free-text query, level (mapped to eventType keyword), and source service
     * tag. Used by the admin "Logs" page as a thin replacement for Kibana when
     * operators just want to skim recent activity.
     */
    public Map<String, Object> searchLogs(String q, String level, String service, int size) {
        IndexCoordinates indices = IndexCoordinates.of(indexPrefix + "-*");
        int limit = Math.min(Math.max(size, 1), 500);

        Query query = Query.of(qb -> qb.bool(b -> {
            if (q != null && !q.isBlank()) {
                b.must(m -> m.multiMatch(mm -> mm
                        .fields(FIELD_EVENT_TYPE, "status", "referenceType", "referenceId")
                        .query(q)));
            }
            if (level != null && !level.isBlank()) {
                b.must(m -> m.term(t -> t.field("status").value(level)));
            }
            if (service != null && !service.isBlank()) {
                b.must(m -> m.term(t -> t.field("referenceType").value(service)));
            }
            return b;
        }));

        var nativeQuery = NativeQuery.builder()
                .withQuery(query)
                .withMaxResults(limit)
                .withSort(s -> s.field(f -> f.field(FIELD_TIMESTAMP).order(SortOrder.Desc)))
                .build();
        SearchHits<AnalyticsEventDocument> hits = esTemplate.search(nativeQuery, AnalyticsEventDocument.class, indices);

        List<Map<String, Object>> content = new ArrayList<>();
        for (SearchHit<AnalyticsEventDocument> h : hits) {
            var d = h.getContent();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", h.getId());
            row.put(FIELD_TIMESTAMP, d.getTimestamp());
            row.put("level", d.getStatus() != null ? d.getStatus() : "INFO");
            row.put("service", d.getReferenceType());
            row.put("message", d.getEventType());
            row.put("userId", d.getUserId());
            row.put("referenceId", d.getReferenceId());
            row.put("amount", d.getAmount());
            row.put("metadata", d.getMetadata());
            content.add(row);
        }
        return Map.of("content", content, "totalElements", hits.getTotalHits());
    }

    /**
     * Daily revenue between [from, to). Sums {@code amount} on
     * {@code paymentCompleted} events grouped by date (UTC).
     */
    public List<Map<String, Object>> revenueByDay(Instant from, Instant to) {
        IndexCoordinates indices = IndexCoordinates.of(indexPrefix + "-*");
        Query query = Query.of(qb -> qb.bool(b -> {
            b.must(m -> m.range(r -> r.date(d -> d
                    .field(FIELD_TIMESTAMP)
                    .gte(from.toString())
                    .lt(to.toString()))));
            b.must(m -> m.term(t -> t.field(FIELD_EVENT_TYPE).value("PAYMENT_COMPLETED")));
            return b;
        }));

        // Pull up to 10k events in the window — fine for a single-tenant dev/prod
        // dashboard. For real scale we'd switch to a date-histogram aggregation.
        var nativeQuery = NativeQuery.builder()
                .withQuery(query)
                .withMaxResults(10_000)
                .build();
        SearchHits<AnalyticsEventDocument> hits = esTemplate.search(nativeQuery, AnalyticsEventDocument.class, indices);

        Map<String, Double> bucket = new LinkedHashMap<>();
        for (SearchHit<AnalyticsEventDocument> h : hits) {
            var d = h.getContent();
            if (d.getTimestamp() == null || d.getAmount() == null) continue;
            String day = LocalDate.ofInstant(d.getTimestamp(), ZoneOffset.UTC).toString();
            bucket.merge(day, d.getAmount().doubleValue(), Double::sum);
        }
        // Fill missing days with 0 so the line chart doesn't have gaps.
        List<Map<String, Object>> out = new ArrayList<>();
        LocalDate cur = LocalDate.ofInstant(from, ZoneOffset.UTC);
        LocalDate end = LocalDate.ofInstant(to, ZoneOffset.UTC);
        while (!cur.isAfter(end)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", cur.toString());
            row.put("revenue", bucket.getOrDefault(cur.toString(), 0d));
            out.add(row);
            cur = cur.plusDays(1);
        }
        return out;
    }
}
