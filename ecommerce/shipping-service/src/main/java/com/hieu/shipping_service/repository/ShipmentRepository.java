package com.hieu.shipping_service.repository;

import com.hieu.shipping_service.entity.ShipmentJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Spring Data repository for {@link ShipmentJpaEntity}. */
@Repository
public interface ShipmentRepository extends JpaRepository<ShipmentJpaEntity, String> {

    Optional<ShipmentJpaEntity> findByOrderId(String orderId);

    List<ShipmentJpaEntity> findByUserId(String userId);

    Page<ShipmentJpaEntity> findByStatus(String status, Pageable pageable);

    Optional<ShipmentJpaEntity> findByTrackingNumber(String trackingNumber);
}
