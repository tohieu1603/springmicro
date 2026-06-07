package com.hieu.analytics_service.kafka;

import com.hieu.analytics_service.document.AnalyticsEventDocument;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Helper to project a raw Kafka payload (HashMap) into a normalized analytics document. */
final class EventPayloadMapper {

    private EventPayloadMapper() {}

    /**
     * Projects a Kafka HashMap payload into AnalyticsEventDocument.
     * Common fields (eventType/userId/orderId/...) are pulled out; everything else stays in metadata.
     */
    static AnalyticsEventDocument toDocument(Map<String, Object> payload, String fallbackEventType, String referenceType) {
        if (payload == null) payload = new HashMap<>();

        String eventType = stringOrNull(payload.get("eventType"));
        if (eventType == null) eventType = fallbackEventType;

        String userId = stringOrNull(firstNonNull(payload.get("userId"), payload.get("user_id")));
        String referenceId = stringOrNull(firstNonNull(
                payload.get("orderId"), payload.get("paymentId"),
                payload.get("productId"), payload.get("id")));

        BigDecimal amount = parseAmount(firstNonNull(
                payload.get("amount"), payload.get("totalAmount"), payload.get("total")));
        String currency = stringOrNull(payload.get("currency"));
        String status = stringOrNull(payload.get("status"));
        Object timestamp = firstNonNull(payload.get("timestamp"), payload.get("occurredAt"));

        Map<String, Object> metadata = new HashMap<>(payload);
        // Strip already-extracted top-level fields to keep metadata light.
        metadata.keySet().removeAll(java.util.List.of(
                "eventType", "userId", "user_id", "orderId", "paymentId", "productId", "id",
                "amount", "totalAmount", "total", "currency", "status", "timestamp", "occurredAt"));

        return AnalyticsEventDocument.builder()
                .eventType(eventType)
                .userId(userId)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .amount(amount)
                .currency(currency)
                .status(status)
                .metadata(metadata.isEmpty() ? null : metadata)
                .timestamp(parseInstant(timestamp))
                .build();
    }

    private static String stringOrNull(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Object firstNonNull(Object... vals) {
        for (Object v : vals) if (v != null) return v;
        return null;
    }

    private static BigDecimal parseAmount(Object v) {
        if (v == null) return null;
        try {
            if (v instanceof Number n) return new BigDecimal(n.toString());
            return new BigDecimal(String.valueOf(v).replaceAll("[^0-9.\\-]", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private static Instant parseInstant(Object v) {
        if (v == null) return Instant.now();
        try {
            if (v instanceof Number n) return Instant.ofEpochMilli(n.longValue());
            return Instant.parse(String.valueOf(v));
        } catch (Exception e) {
            return Instant.now();
        }
    }
}
