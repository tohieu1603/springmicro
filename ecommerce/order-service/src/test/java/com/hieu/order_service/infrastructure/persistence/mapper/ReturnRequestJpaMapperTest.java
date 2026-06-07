package com.hieu.order_service.infrastructure.persistence.mapper;

import com.hieu.order_service.domain.model.order.ReturnRequest;
import com.hieu.order_service.domain.model.order.valueobject.*;
import com.hieu.order_service.infrastructure.persistence.jpa.entities.ReturnRequestJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure unit tests for ReturnRequest aggregate <-> JPA entity mapping. */
class ReturnRequestJpaMapperTest {

    private final ReturnRequestJpaMapper mapper = new ReturnRequestJpaMapper();
    private static final String USER = UUID.randomUUID().toString();

    private ReturnRequest reconstitute(String id, RefundAmount refund) {
        return ReturnRequest.reconstitute(
                ReturnRequestId.of(id),
                OrderId.of("00000000-0000-0000-0000-000000000055"),
                UserId.of(USER),
                ReturnReason.of("defective"),
                ReturnType.REFUND,
                ReturnStatus.APPROVED,
                refund,
                "approved by admin",
                "[\"a.png\"]",
                Instant.parse("2024-02-01T00:00:00Z"),
                Instant.parse("2024-02-02T00:00:00Z"));
    }

    @Test
    @DisplayName("toJpa maps every field including refund amount")
    void toJpa_mapsAllFields() {
        var rr = reconstitute("00000000-0000-0000-0000-000000000010", RefundAmount.of(new BigDecimal("99.50")));

        var e = mapper.toJpa(rr);

        assertThat(e.getId()).isEqualTo("00000000-0000-0000-0000-000000000010");
        assertThat(e.getOrderId()).isEqualTo("00000000-0000-0000-0000-000000000055");
        assertThat(e.getUserId()).isEqualTo(USER);
        assertThat(e.getReason()).isEqualTo("defective");
        assertThat(e.getReturnType()).isEqualTo("REFUND");
        assertThat(e.getStatus()).isEqualTo("APPROVED");
        assertThat(e.getRefundAmount()).isEqualByComparingTo("99.50");
        assertThat(e.getAdminNote()).isEqualTo("approved by admin");
        assertThat(e.getImages()).isEqualTo("[\"a.png\"]");
        assertThat(e.getCreatedAt()).isEqualTo(Instant.parse("2024-02-01T00:00:00Z"));
        assertThat(e.getUpdatedAt()).isEqualTo(Instant.parse("2024-02-02T00:00:00Z"));
    }

    @Test
    @DisplayName("toJpa maps a null refund amount to a null column")
    void toJpa_nullRefund() {
        var e = mapper.toJpa(reconstitute("00000000-0000-0000-0000-000000000010", null));

        assertThat(e.getRefundAmount()).isNull();
    }

    @Test
    @DisplayName("toDomain rebuilds the aggregate with refund amount")
    void toDomain_withRefund() {
        var e = mapper.toJpa(reconstitute("00000000-0000-0000-0000-000000000010", RefundAmount.of(new BigDecimal("12.00"))));

        var domain = mapper.toDomain(e);

        assertThat(domain.getId().value()).isEqualTo("00000000-0000-0000-0000-000000000010");
        assertThat(domain.getOrderId().value()).isEqualTo("00000000-0000-0000-0000-000000000055");
        assertThat(domain.getUserId().value()).isEqualTo(USER);
        assertThat(domain.getReason().value()).isEqualTo("defective");
        assertThat(domain.getReturnType()).isEqualTo(ReturnType.REFUND);
        assertThat(domain.getStatus()).isEqualTo(ReturnStatus.APPROVED);
        assertThat(domain.getRefundAmount().amount()).isEqualByComparingTo("12.00");
        assertThat(domain.getImages()).isEqualTo("[\"a.png\"]");
    }

    @Test
    @DisplayName("toDomain maps a null refund column to a null value object")
    void toDomain_nullRefund() {
        var e = mapper.toJpa(reconstitute("00000000-0000-0000-0000-000000000010", null));

        var domain = mapper.toDomain(e);

        assertThat(domain.getRefundAmount()).isNull();
    }

    @Test
    @DisplayName("round trip preserves the aggregate")
    void roundTrip() {
        var original = reconstitute("00000000-0000-0000-0000-000000000033", RefundAmount.of(new BigDecimal("7.25")));

        var back = mapper.toDomain(mapper.toJpa(original));

        assertThat(back.getId().value()).isEqualTo("00000000-0000-0000-0000-000000000033");
        assertThat(back.getAdminNote()).isEqualTo(original.getAdminNote());
        assertThat(back.getRefundAmount().amount()).isEqualByComparingTo("7.25");
    }

    @Test
    @DisplayName("syncGeneratedIds copies the DB id onto the aggregate")
    void syncGeneratedIds() {
        var rr = ReturnRequest.create(OrderId.of("00000000-0000-0000-0000-000000000001"), UserId.of(USER),
                ReturnReason.of("r"), ReturnType.EXCHANGE, null);
        assertThat(rr.getId()).isNull();

        var saved = new ReturnRequestJpaEntity();
        saved.setId("00000000-0000-0000-0000-000000004242");

        mapper.syncGeneratedIds(rr, saved);

        assertThat(rr.getId().value()).isEqualTo("00000000-0000-0000-0000-000000004242");
    }
}
