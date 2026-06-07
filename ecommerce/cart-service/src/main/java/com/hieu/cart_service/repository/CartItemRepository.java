package com.hieu.cart_service.repository;

import com.hieu.cart_service.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** JPA repository for {@link CartItem}. */
public interface CartItemRepository extends JpaRepository<CartItem, String> {

    List<CartItem> findAllByUserId(String userId);

    Optional<CartItem> findByUserIdAndVariantId(String userId, String variantId);

    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.userId = :userId AND c.variantId = :variantId")
    int deleteByUserIdAndVariantId(@Param("userId") String userId, @Param("variantId") String variantId);

    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.userId = :userId")
    int deleteAllByUserId(@Param("userId") String userId);

    /** Used by Kafka consumer when product is deleted/inactive. Returns affected userIds. */
    @Query("SELECT DISTINCT c.userId FROM CartItem c WHERE c.productId = :productId")
    List<String> findUserIdsByProductId(@Param("productId") String productId);

    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.productId = :productId")
    int deleteAllByProductId(@Param("productId") String productId);
}
