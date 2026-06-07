package com.hieu.flash_sale_service.repository;

import com.hieu.flash_sale_service.entity.FlashSaleParticipation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repository for per-user participation records. */
public interface FlashSaleParticipationRepository extends JpaRepository<FlashSaleParticipation, String> {

    /**
     * Returns total quantity claimed by a user in a sale.
     * Returns 0 if no participation exists.
     */
    @Query("SELECT COALESCE(SUM(p.quantity), 0) FROM FlashSaleParticipation p " +
           "WHERE p.saleId = :saleId AND p.userId = :userId")
    int sumQuantityBySaleIdAndUserId(@Param("saleId") String saleId, @Param("userId") String userId);
}
