package com.hieu.order_service.infrastructure.persistence.mapper;

import com.hieu.order_service.domain.model.order.IdempotencyRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure unit tests for IdempotencyRecord <-> JPA entity mapping. */
class IdempotencyJpaMapperTest {

    private final IdempotencyJpaMapper mapper = new IdempotencyJpaMapper();

    private IdempotencyRecord completedRecord() {
        return IdempotencyRecord.reconstitute(
                "key-1",
                "00000000-0000-0000-0000-000000000123",
                IdempotencyRecord.Status.COMPLETED,
                "{\"id\":123}",
                Instant.parse("2024-03-01T00:00:00Z"),
                Instant.parse("2024-03-01T00:30:00Z"),
                Instant.parse("2024-03-01T00:00:01Z"));
    }

    @Test
    @DisplayName("toJpa maps every field, status as name")
    void toJpa_mapsAllFields() {
        var e = mapper.toJpa(completedRecord());

        assertThat(e.getIdempotencyKey()).isEqualTo("key-1");
        assertThat(e.getOrderId()).isEqualTo(123L);
        assertThat(e.getStatus()).isEqualTo("COMPLETED");
        assertThat(e.getResponseBody()).isEqualTo("{\"id\":123}");
        assertThat(e.getCreatedAt()).isEqualTo(Instant.parse("2024-03-01T00:00:00Z"));
        assertThat(e.getExpiresAt()).isEqualTo(Instant.parse("2024-03-01T00:30:00Z"));
        assertThat(e.getProcessingStartedAt()).isEqualTo(Instant.parse("2024-03-01T00:00:01Z"));
    }

    @Test
    @DisplayName("toDomain parses status enum and maps every field")
    void toDomain_mapsAllFields() {
        var domain = mapper.toDomain(mapper.toJpa(completedRecord()));

        assertThat(domain.getIdempotencyKey()).isEqualTo("key-1");
        assertThat(domain.getOrderId()).isEqualTo(123L);
        assertThat(domain.getStatus()).isEqualTo(IdempotencyRecord.Status.COMPLETED);
        assertThat(domain.getResponseBody()).isEqualTo("{\"id\":123}");
        assertThat(domain.getExpiresAt()).isEqualTo(Instant.parse("2024-03-01T00:30:00Z"));
    }

    @Test
    @DisplayName("round trip preserves a PROCESSING record with null order id and body")
    void roundTrip_processing() {
        var processing = IdempotencyRecord.reconstitute(
                "k2", null, IdempotencyRecord.Status.PROCESSING, null,
                Instant.parse("2024-03-02T00:00:00Z"),
                Instant.parse("2024-03-02T00:30:00Z"),
                Instant.parse("2024-03-02T00:00:00Z"));

        var back = mapper.toDomain(mapper.toJpa(processing));

        assertThat(back.getStatus()).isEqualTo(IdempotencyRecord.Status.PROCESSING);
        assertThat(back.getOrderId()).isNull();
        assertThat(back.getResponseBody()).isNull();
    }
}
