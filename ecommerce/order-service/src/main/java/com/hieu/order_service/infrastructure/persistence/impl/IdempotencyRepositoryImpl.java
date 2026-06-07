package com.hieu.order_service.infrastructure.persistence.impl;

import com.hieu.order_service.domain.model.order.IdempotencyRecord;
import com.hieu.order_service.domain.repository.IdempotencyRepository;
import com.hieu.order_service.infrastructure.persistence.jpa.repositories.IdempotencyJpaRepository;
import com.hieu.order_service.infrastructure.persistence.mapper.IdempotencyJpaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class IdempotencyRepositoryImpl implements IdempotencyRepository {

    private final IdempotencyJpaRepository jpa;
    private final IdempotencyJpaMapper mapper;

    @Override
    public IdempotencyRecord save(IdempotencyRecord record) {
        jpa.save(mapper.toJpa(record));
        return record;
    }

    @Override
    public Optional<IdempotencyRecord> findByKey(String key) {
        return jpa.findById(key).map(mapper::toDomain);
    }
}
