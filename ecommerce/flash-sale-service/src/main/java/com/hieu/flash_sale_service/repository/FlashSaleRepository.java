package com.hieu.flash_sale_service.repository;

import com.hieu.flash_sale_service.entity.FlashSaleJpaEntity;
import com.hieu.flash_sale_service.entity.FlashSaleStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Spring Data repository for {@link FlashSaleJpaEntity}. */
public interface FlashSaleRepository extends JpaRepository<FlashSaleJpaEntity, String> {

    @Query("SELECT f FROM FlashSaleJpaEntity f WHERE f.status = :status AND f.startTime <= :now AND f.endTime >= :now")
    Page<FlashSaleJpaEntity> findActiveSales(
            @Param("status") FlashSaleStatus status,
            @Param("now") Instant now,
            Pageable pageable);

    @Query("SELECT f FROM FlashSaleJpaEntity f WHERE f.status = :status")
    List<FlashSaleJpaEntity> findAllByStatus(@Param("status") FlashSaleStatus status);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT f FROM FlashSaleJpaEntity f WHERE f.id = :id")
    Optional<FlashSaleJpaEntity> findByIdWithReadLock(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FlashSaleJpaEntity f WHERE f.id = :id")
    Optional<FlashSaleJpaEntity> findByIdWithWriteLock(@Param("id") String id);
}
