package com.hieu.order_service.domain.repository;

import com.hieu.order_service.domain.model.order.IdempotencyRecord;

import java.util.Optional;

public interface IdempotencyRepository {
    IdempotencyRecord save(IdempotencyRecord record);
    Optional<IdempotencyRecord> findByKey(String key);
}
