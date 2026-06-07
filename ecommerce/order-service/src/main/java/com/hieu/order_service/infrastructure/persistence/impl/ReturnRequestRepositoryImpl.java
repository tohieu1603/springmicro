package com.hieu.order_service.infrastructure.persistence.impl;

import com.hieu.order_service.domain.model.order.ReturnRequest;
import com.hieu.order_service.domain.model.order.valueobject.OrderId;
import com.hieu.order_service.domain.model.order.valueobject.ReturnRequestId;
import com.hieu.order_service.domain.model.order.valueobject.UserId;
import com.hieu.order_service.domain.repository.ReturnRequestRepository;
import com.hieu.order_service.infrastructure.persistence.jpa.repositories.ReturnRequestJpaRepository;
import com.hieu.order_service.infrastructure.persistence.mapper.ReturnRequestJpaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReturnRequestRepositoryImpl implements ReturnRequestRepository {

    private final ReturnRequestJpaRepository jpa;
    private final ReturnRequestJpaMapper mapper;

    @Override
    public ReturnRequest save(ReturnRequest rr) {
        var saved = jpa.saveAndFlush(mapper.toJpa(rr));
        mapper.syncGeneratedIds(rr, saved);
        return rr;
    }

    @Override
    public Optional<ReturnRequest> findById(ReturnRequestId id) {
        return jpa.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Page<ReturnRequest> findByUserId(UserId userId, Pageable pageable) {
        return jpa.findByUserId(userId.value(), pageable).map(mapper::toDomain);
    }

    @Override
    public Optional<ReturnRequest> findByOrderId(OrderId orderId) {
        return jpa.findByOrderId(orderId.value()).map(mapper::toDomain);
    }
}
