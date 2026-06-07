package com.hieu.inventory_service.repository;

import com.hieu.inventory_service.entity.InventoryEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Data access for {@link InventoryEntity}. */
@Repository
public interface InventoryRepository extends JpaRepository<InventoryEntity, String> {

    Optional<InventoryEntity> findByProductId(String productId);

    Optional<InventoryEntity> findBySku(String sku);

    boolean existsByProductId(String productId);

    boolean existsBySku(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryEntity i WHERE i.productId IN :ids ORDER BY i.productId")
    List<InventoryEntity> findAllByProductIdInWithLock(@Param("ids") List<String> ids);
}
