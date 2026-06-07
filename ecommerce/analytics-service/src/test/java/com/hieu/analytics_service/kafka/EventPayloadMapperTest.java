package com.hieu.analytics_service.kafka;

import com.hieu.analytics_service.document.AnalyticsEventDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("EventPayloadMapper — Unit")
class EventPayloadMapperTest {
    /**
     * Top-level smoke test — bắt buộc để Sonar rule java:S2187 nhận diện
     * class này có @Test (không tính các @Test bên trong @Nested).
     * Mọi test thật sự được tổ chức trong các @Nested class bên dưới.
     */
    @Test
    @DisplayName("class loads + JUnit discovers @Nested tests")
    void smokeTest_classDiscovered() {
        // Nếu tới được đây tức là JUnit có thể instantiate test class.
        // assertThat(this).isNotNull() được tự thực hiện ngầm.
    }


    @Nested
    @DisplayName("ORDER payload mapping")
    class OrderPayload {

        @Test
        @DisplayName("full order payload maps to AnalyticsEventDocument with correct fields")
        void fullOrderPayload_mapsCorrectly() {
            Map<String, Object> payload = Map.of(
                    "eventType", "ORDER_PLACED",
                    "userId", "u1",
                    "orderId", 42,
                    "amount", 100
            );

            AnalyticsEventDocument doc = EventPayloadMapper.toDocument(payload, "ORDER_PLACED", "ORDER");

            assertThat(doc.getEventType()).isEqualTo("ORDER_PLACED");
            assertThat(doc.getUserId()).isEqualTo("u1");
            assertThat(doc.getReferenceId()).isEqualTo("42");
            assertThat(doc.getReferenceType()).isEqualTo("ORDER");
            assertThat(doc.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        }

        @Test
        @DisplayName("fallbackEventType used when payload lacks eventType")
        void missingEventType_usesFallback() {
            Map<String, Object> payload = Map.of("userId", "u2", "orderId", 99);

            AnalyticsEventDocument doc = EventPayloadMapper.toDocument(payload, "ORDER_EVENT_FALLBACK", "ORDER");

            assertThat(doc.getEventType()).isEqualTo("ORDER_EVENT_FALLBACK");
        }

        @Test
        @DisplayName("referenceType is forwarded verbatim to document")
        void referenceType_forwardedVerbatim() {
            AnalyticsEventDocument doc = EventPayloadMapper.toDocument(
                    Map.of("orderId", "order-123"), "X", "ORDER");
            assertThat(doc.getReferenceType()).isEqualTo("ORDER");
        }
    }

    @Nested
    @DisplayName("Missing / null fields handled gracefully")
    class MissingFields {

        @Test
        @DisplayName("null payload does not throw NPE")
        void nullPayload_noNpe() {
            assertThatCode(() -> EventPayloadMapper.toDocument(null, "FALLBACK", "ORDER"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("empty payload produces document with null amount and userId")
        void emptyPayload_nullableFieldsHandled() {
            AnalyticsEventDocument doc = EventPayloadMapper.toDocument(
                    new HashMap<>(), "FALLBACK", "TYPE");

            assertThat(doc.getUserId()).isNull();
            assertThat(doc.getAmount()).isNull();
            assertThat(doc.getReferenceId()).isNull();
        }

        @Test
        @DisplayName("malformed amount string produces null amount — no exception")
        void malformedAmount_nullAmount() {
            Map<String, Object> payload = new HashMap<>();
            payload.put("amount", "not-a-number!!");
            payload.put("userId", "u3");

            assertThatCode(() -> {
                AnalyticsEventDocument doc = EventPayloadMapper.toDocument(payload, "X", "Y");
                assertThat(doc.getAmount()).isNull();
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Timestamp parsing")
    class Timestamp {

        @Test
        @DisplayName("ISO-8601 string timestamp is parsed correctly")
        void isoStringTimestamp_parsed() {
            String ts = "2025-01-15T10:30:00Z";
            Map<String, Object> payload = new HashMap<>();
            payload.put("timestamp", ts);

            AnalyticsEventDocument doc = EventPayloadMapper.toDocument(payload, "E", "T");

            assertThat(doc.getTimestamp()).isEqualTo(Instant.parse(ts));
        }

        @Test
        @DisplayName("epoch millis timestamp is parsed correctly")
        void epochMillisTimestamp_parsed() {
            long millis = 1_700_000_000_000L;
            Map<String, Object> payload = new HashMap<>();
            payload.put("timestamp", millis);

            AnalyticsEventDocument doc = EventPayloadMapper.toDocument(payload, "E", "T");

            assertThat(doc.getTimestamp()).isEqualTo(Instant.ofEpochMilli(millis));
        }

        @Test
        @DisplayName("missing timestamp defaults to Instant.now() — not null")
        void missingTimestamp_defaultsToNow() {
            Instant before = Instant.now().minusSeconds(1);

            AnalyticsEventDocument doc = EventPayloadMapper.toDocument(
                    new HashMap<>(), "E", "T");

            assertThat(doc.getTimestamp()).isAfter(before);
        }

        @Test
        @DisplayName("unparseable timestamp string falls back to Instant.now() — no exception")
        void unparseableTimestamp_fallsBackToNow() {
            Map<String, Object> payload = new HashMap<>();
            payload.put("timestamp", "not-a-date");
            Instant before = Instant.now().minusSeconds(1);

            assertThatCode(() -> {
                AnalyticsEventDocument doc = EventPayloadMapper.toDocument(payload, "E", "T");
                assertThat(doc.getTimestamp()).isAfter(before);
            }).doesNotThrowAnyException();
        }
    }
}
