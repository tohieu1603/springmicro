package com.hieu.auth_service.infrastructure.persistence.impl;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.hieu.auth_service.domain.events.DomainEventPublisher;
import com.hieu.auth_service.domain.models.role.vo.RoleId;
import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.vo.Email;
import com.hieu.auth_service.domain.models.user.vo.GoogleSub;
import com.hieu.auth_service.domain.models.user.vo.UserId;
import com.hieu.auth_service.domain.models.user.vo.Username;
import com.hieu.auth_service.domain.repositories.UserRepository;
import com.hieu.auth_service.infrastructure.persistence.jpa.entities.RoleJpaEntity;
import com.hieu.auth_service.infrastructure.persistence.jpa.entities.UserJpaEntity;
import com.hieu.auth_service.infrastructure.persistence.jpa.repositories.RoleJpaRepository;
import com.hieu.auth_service.infrastructure.persistence.jpa.repositories.UserJpaRepository;
import com.hieu.auth_service.infrastructure.persistence.mapper.UserJpaMapper;

import lombok.RequiredArgsConstructor;

/**
 * Spring Data JPA adapter for {@link UserRepository}.
 *
 * <p>Bridges the domain aggregate onto its JPA entity via {@link UserJpaMapper}.
 * On every save the adapter drains the aggregate's domain events and forwards them
 * to {@link DomainEventPublisher} — subscribers listening at
 * {@code @TransactionalEventListener(AFTER_COMMIT)} see events only when the surrounding
 * transaction actually commits.
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final RoleJpaRepository roleRepository;
    private final UserJpaMapper mapper;
    private final DomainEventPublisher eventPublisher;

    /**
     * Persists or updates a {@link User} aggregate.
     *
     * <p>Manual-ID aggregates: we check existence first to decide between INSERT and UPDATE.
     * Roles are resolved to managed entities so Hibernate maintains the {@code user_roles}
     * join table without orphan-removal surprises.
     *
     * @param user domain aggregate to persist; never null
     * @return reconstituted aggregate reflecting the saved row
     */
    @Override
    @Transactional
    public User save(User user) {
        boolean isNew = !jpaRepository.existsById(user.getId().value());

        Set<String> roleIds = user.getRoles().stream()
                .map(RoleId::value)
                .collect(Collectors.toSet());
        Set<RoleJpaEntity> roleEntities = roleRepository.findByIdIn(roleIds);

        UserJpaEntity saved = jpaRepository.save(mapper.toJpaEntity(user, roleEntities, isNew));

        // Drain events after persistence succeeds so visibility aligns with commit semantics.
        user.pullDomainEvents().forEach(eventPublisher::publish);

        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UserId userId) {
        return jpaRepository.findByIdWithRoles(userId.value()).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(Username username) {
        return jpaRepository.findByUsernameWithRoles(username.value()).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(Email email) {
        return jpaRepository.findByEmailWithRoles(email.value()).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByGoogleSub(GoogleSub googleSub) {
        return jpaRepository.findByGoogleSubWithRoles(googleSub.value()).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(Username username) {
        return jpaRepository.existsByUsername(username.value());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(Email email) {
        return jpaRepository.existsByEmail(email.value());
    }

    @Override
    @Transactional
    public void delete(User user) {
        if (user.getId() != null) {
            jpaRepository.deleteById(user.getId().value());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByIdWithRoles(UserId userId) {
        return jpaRepository.findByIdWithRoles(userId.value()).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsernameWithRoles(Username username) {
        return jpaRepository.findByUsernameWithRoles(username.value()).map(mapper::toDomain);
    }

    /**
     * Two-phase keyset pagination: first fetch ids, then fetch full entities with roles,
     * preventing N+1 without blowing up memory via {@code LEFT JOIN FETCH} on an unbounded set.
     */
    @Override
    @Transactional(readOnly = true)
    public List<User> findAfterCursor(Instant cursorCreatedAt, String cursorId, int limit) {
        var pageable = org.springframework.data.domain.PageRequest.of(0, limit);
        // First-page vs subsequent-page split — Postgres can't infer the type of a
        // null Instant parameter inside `:p IS NULL` predicates.
        List<String> ids = (cursorCreatedAt == null)
                ? jpaRepository.findFirstPageIds(pageable)
                : jpaRepository.findIdsAfterCursor(cursorCreatedAt, cursorId, pageable);
        if (ids.isEmpty()) return Collections.emptyList();
        return jpaRepository.findAllByIdInWithRoles(ids).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
