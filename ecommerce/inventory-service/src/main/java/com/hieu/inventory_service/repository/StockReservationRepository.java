package com.hieu.inventory_service.repository;

import com.hieu.inventory_service.entity.StockReservationRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Data access for {@link StockReservationRecord}. */
@Repository
public interface StockReservationRepository extends JpaRepository<StockReservationRecord, String> {

    Optional<StockReservationRecord> findByOrderId(String orderId);

    /**
     * Locks the reservation row to serialize concurrent confirm/release on the same
     * orderId — without this, both threads see ACTIVE and double-apply the side effect.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM StockReservationRecord r WHERE r.orderId = :orderId")
    Optional<StockReservationRecord> findByOrderIdForUpdate(@Param("orderId") String orderId);
}
