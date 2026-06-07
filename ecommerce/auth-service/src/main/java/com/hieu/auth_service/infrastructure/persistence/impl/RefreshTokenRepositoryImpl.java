package com.hieu.auth_service.infrastructure.persistence.impl;

import com.hieu.auth_service.domain.events.DomainEventPublisher;
import com.hieu.auth_service.domain.models.refreshtoken.RefreshToken;
import com.hieu.auth_service.domain.models.refreshtoken.vo.TokenFamily;
import com.hieu.auth_service.domain.models.refreshtoken.vo.TokenId;
import com.hieu.auth_service.domain.models.refreshtoken.vo.TokenValue;
import com.hieu.auth_service.domain.models.user.vo.UserId;
import com.hieu.auth_service.domain.repositories.RefreshTokenRepository;
import com.hieu.auth_service.infrastructure.persistence.jpa.entities.RefreshTokenJpaEntity;
import com.hieu.auth_service.infrastructure.persistence.jpa.entities.UserJpaEntity;
import com.hieu.auth_service.infrastructure.persistence.jpa.repositories.RefreshTokenJpaRepository;
import com.hieu.auth_service.infrastructure.persistence.jpa.repositories.UserJpaRepository;
import com.hieu.auth_service.infrastructure.persistence.mapper.RefreshTokenJpaMapper;
import com.hieu.auth_service.infrastructure.security.RefreshTokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA adapter for {@link RefreshTokenRepository}.
 *
 * <p>Renamed from {@code RefreshTokenImpl} (which lacked the {@code RepositoryImpl} suffix
 * and mixed mapping with persistence logic). This implementation routes all mapping
 * through {@link RefreshTokenJpaMapper} and drains domain events on save.
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final RefreshTokenJpaMapper mapper;
    private final DomainEventPublisher eventPublisher;
    private final RefreshTokenHasher tokenHasher;

    /**
     * Persists or updates a refresh-token aggregate. Resolves the owning user entity so
     * Hibernate can populate the mandatory {@code user_id} FK without detached-entity surprises.
     *
     * <p>On INSERT the raw token secret is replaced by its SHA-256 digest before it touches the
     * database — only the digest is ever stored. On UPDATE the aggregate was rehydrated via
     * {@link #toDigest} lookups, so its value is already the digest and is persisted as-is.
     */
    @Override
    @Transactional
    public RefreshToken save(RefreshToken token) {
        UserJpaEntity userRef = userJpaRepository.findById(token.getUserId().value())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cannot save refresh token — owning user not found: " + token.getUserId().value()));

        boolean isNew = !jpaRepository.existsById(token.getId().value());
        RefreshTokenJpaEntity entity = mapper.toJpaEntity(token, userRef, isNew);
        if (isNew) {
            entity.setToken(tokenHasher.hash(token.getValue().value()));
        }
        RefreshTokenJpaEntity saved = jpaRepository.save(entity);

        token.pullDomainEvents().forEach(eventPublisher::publish);
        return mapper.toDomain(saved);
    }

    /** Hashes a presented raw token so it can be matched against the stored digest. */
    private String toDigest(TokenValue tokenValue) {
        return tokenHasher.hash(tokenValue.value());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findById(TokenId tokenId) {
        return jpaRepository.findById(tokenId.value()).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByTokenValue(TokenValue tokenValue) {
        return jpaRepository.findByToken(toDigest(tokenValue)).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<RefreshToken> findByTokenValueForUpdate(TokenValue tokenValue) {
        return jpaRepository.findByTokenForUpdate(toDigest(tokenValue)).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefreshToken> findByUserId(UserId userId) {
        return jpaRepository.findByUserId(userId.value()).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefreshToken> findValidTokensByUserId(UserId userId) {
        return jpaRepository.findValidTokensByUserId(userId.value(), Instant.now()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefreshToken> findByFamily(TokenFamily family) {
        return jpaRepository.findByFamily(family.value()).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void delete(RefreshToken token) {
        if (token.getId() != null) {
            jpaRepository.deleteById(token.getId().value());
        }
    }

    @Override
    @Transactional
    public void deleteByUserId(UserId userId) {
        jpaRepository.deleteByUserId(userId.value());
    }

    @Override
    @Transactional
    public int deleteExpiredTokens() {
        return jpaRepository.deleteExpiredTokens(Instant.now());
    }

    @Override
    @Transactional
    public void revokeAllTokensForUser(UserId userId) {
        jpaRepository.revokeAllTokensForUser(userId.value(), Instant.now());
    }
}
