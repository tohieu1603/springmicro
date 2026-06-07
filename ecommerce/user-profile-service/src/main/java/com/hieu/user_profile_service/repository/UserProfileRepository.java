package com.hieu.user_profile_service.repository;

import com.hieu.user_profile_service.entity.UserProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfileJpaEntity, String> {

    Optional<UserProfileJpaEntity> findByEmail(String email);

    @Modifying
    @Query(value = """
            INSERT INTO user_profiles (user_id, email, first_name, last_name, created_at, updated_at, version)
            VALUES (:userId, :email, :firstName, :lastName, now(), now(), 0)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    void insertIfAbsent(@Param("userId") String userId,
                        @Param("email") String email,
                        @Param("firstName") String firstName,
                        @Param("lastName") String lastName);
}
