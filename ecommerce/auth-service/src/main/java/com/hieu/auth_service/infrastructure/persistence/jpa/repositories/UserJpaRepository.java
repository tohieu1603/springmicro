package com.hieu.auth_service.infrastructure.persistence.jpa.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hieu.auth_service.infrastructure.persistence.jpa.entities.UserJpaEntity;


/**
 * Spring Data JPA Repository for UserJpaEntity
 */
@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, String> {

    Optional<UserJpaEntity> findByUsername(String username);

    Optional<UserJpaEntity> findByEmail(String email);

    @Query("SELECT u FROM UserJpaEntity u LEFT JOIN FETCH u.roles WHERE u.email = :email")
    Optional<UserJpaEntity> findByEmailWithRoles(@Param("email") String email);

    @Query("SELECT u FROM UserJpaEntity u LEFT JOIN FETCH u.roles WHERE u.googleSub = :sub")
    Optional<UserJpaEntity> findByGoogleSubWithRoles(@Param("sub") String googleSub);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM UserJpaEntity u LEFT JOIN FETCH u.roles WHERE u.id = :id")
    Optional<UserJpaEntity> findByIdWithRoles(@Param("id") String id);

    @Query("SELECT u FROM UserJpaEntity u LEFT JOIN FETCH u.roles WHERE u.username = :username")
    Optional<UserJpaEntity> findByUsernameWithRoles(@Param("username") String username);

    /**
     * Cursor pagination step 1: fetch user ids with id > cursor, ordered by id ASC.
     * Pass {@code Pageable.ofSize(n)} to limit rows.
     */     /**
     * Keyset pagination — first page (no cursor).
     * Split from the with-cursor variant because Postgres can't infer the type of
     * a null parameter in a {@code :p IS NULL OR u.col < :p} predicate.
     */
    @Query("SELECT u.id FROM UserJpaEntity u ORDER BY u.createdAt DESC, u.id DESC")
    List<String> findFirstPageIds(Pageable pageable);

    /**
     * Keyset pagination — subsequent pages. Tie-breaks on id when createdAt equals.
     */
    @Query("SELECT u.id FROM UserJpaEntity u " +
            "WHERE u.createdAt < :cursorCreatedAt " +
            "   OR (u.createdAt = :cursorCreatedAt AND u.id < :cursorId) " +
            "ORDER BY u.createdAt DESC, u.id DESC")
    List<String> findIdsAfterCursor(
            @Param("cursorCreatedAt") java.time.Instant cursorCreatedAt,
            @Param("cursorId") String cursorId,
            Pageable pageable);

    /**
     * Cursor pagination step 2: fetch users with roles to avoid N+1.
     */
    @Query("SELECT DISTINCT u FROM UserJpaEntity u LEFT JOIN FETCH u.roles WHERE u.id IN :ids ORDER BY u.createdAt DESC, u.id DESC")
    List<UserJpaEntity> findAllByIdInWithRoles(@Param("ids") List<String> ids);
}