package com.hieu.user_profile_service.repository;

import com.hieu.user_profile_service.entity.AddressJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<AddressJpaEntity, String> {

    List<AddressJpaEntity> findByUserProfile_UserId(String userId);

    Optional<AddressJpaEntity> findByIdAndUserProfile_UserId(String id, String userId);

    @Modifying
    @Query("UPDATE AddressJpaEntity a SET a.isDefault = false WHERE a.userProfile.userId = :userId")
    void clearDefaultForUser(@Param("userId") String userId);
}
