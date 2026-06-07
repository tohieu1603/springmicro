package com.hieu.inventory_service.repository;

import com.hieu.inventory_service.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, String> {

    Page<StockMovement> findByProductIdOrderByCreatedAtDesc(String productId, Pageable pageable);

    Page<StockMovement> findBySkuOrderByCreatedAtDesc(String sku, Pageable pageable);

    Page<StockMovement> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
