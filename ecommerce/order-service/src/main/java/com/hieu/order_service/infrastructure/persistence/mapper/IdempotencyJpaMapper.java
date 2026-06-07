package com.hieu.order_service.infrastructure.persistence.mapper;

import com.hieu.order_service.domain.model.order.IdempotencyRecord;
import com.hieu.order_service.infrastructure.persistence.jpa.entities.IdempotencyJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyJpaMapper {

    public IdempotencyJpaEntity toJpa(IdempotencyRecord record) {
        var e = new IdempotencyJpaEntity();
        e.setIdempotencyKey(record.getIdempotencyKey());
        e.setOrderId(record.getOrderId());
        e.setStatus(record.getStatus().name());
        e.setResponseBody(record.getResponseBody());
        e.setCreatedAt(record.getCreatedAt());
        e.setExpiresAt(record.getExpiresAt());
        e.setProcessingStartedAt(record.getProcessingStartedAt());
        return e;
    }

    public IdempotencyRecord toDomain(IdempotencyJpaEntity e) {
        return IdempotencyRecord.reconstitute(
                e.getIdempotencyKey(),
                e.getOrderId(),
                IdempotencyRecord.Status.valueOf(e.getStatus()),
                e.getResponseBody(),
                e.getCreatedAt(),
                e.getExpiresAt(),
                e.getProcessingStartedAt()
        );
    }
}
