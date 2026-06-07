package com.hieu.order_service.infrastructure.persistence.jpa.repositories;

import com.hieu.order_service.infrastructure.persistence.jpa.entities.ReturnRequestJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReturnRequestJpaRepository extends JpaRepository<ReturnRequestJpaEntity, String> {

    Page<ReturnRequestJpaEntity> findByUserId(String userId, Pageable pageable);

    Optional<ReturnRequestJpaEntity> findByOrderId(String orderId);
}
