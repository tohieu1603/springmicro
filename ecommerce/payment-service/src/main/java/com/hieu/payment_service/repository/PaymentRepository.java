package com.hieu.payment_service.repository;

import com.hieu.payment_service.entity.PaymentJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentJpaEntity, String> {

    Optional<PaymentJpaEntity> findByOrderId(String orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentJpaEntity p WHERE p.orderId = :orderId")
    Optional<PaymentJpaEntity> findByOrderIdWithLock(@Param("orderId") String orderId);

    Optional<PaymentJpaEntity> findByIdempotencyKey(String idempotencyKey);

    Page<PaymentJpaEntity> findByUserId(String userId, Pageable pageable);
}
