package com.hieu.auth_service.domain.services;

import com.hieu.auth_service.domain.models.refreshtoken.RefreshToken;
import com.hieu.auth_service.domain.models.refreshtoken.exceptions.TokenOwnershipException;
import com.hieu.auth_service.domain.models.refreshtoken.exceptions.TokenReuseDetectedException;
import com.hieu.auth_service.domain.models.refreshtoken.vo.RevokedReason;
import com.hieu.auth_service.domain.models.refreshtoken.vo.TokenFamily;
import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.vo.UserId;
import com.hieu.auth_service.domain.repositories.RefreshTokenRepository;

/**
 * Cross-aggregate token business rules: issue, rotate, reuse-detection, family revocation.
 *
 * <p>Stateless and framework-free — lives in the domain layer so the same rules apply
 * whether callers come from REST, gRPC, Kafka, or tests.
 */
public class TokenDomainService {

    /**
     * Issue the very first refresh token of a login session.
     *
     * @throws com.hieu.auth_service.domain.models.user.exceptions.AccountNotUsableException
     *         when the user cannot currently authenticate (specific reason carried in the exception)
     */
    public RefreshToken issueForUser(User user, int expiryDays) {
        user.ensureAuthenticatable();
        return RefreshToken.create(user.getId(), expiryDays);
    }

    /**
     * Rotate: revoke old, issue next-generation.
     * Presented-but-already-revoked tokens are treated as theft — the full family is
     * revoked and {@link TokenReuseDetectedException} is raised.
     */
    public RefreshToken rotate(RefreshToken old, int expiryDays, RefreshTokenRepository repository) {
        if (old.isReuseAttempt()) {
            revokeFamily(old.getFamily(), repository);
            throw new TokenReuseDetectedException(
                    old.getFamily().value(),
                    old.getGeneration().value());
        }

        old.verifyValidity();
        old.revoke(RevokedReason.NORMAL);
        repository.save(old);

        return RefreshToken.rotate(old, expiryDays);
    }

    /** Marks every live token in a family as revoked with reason FAMILY_REVOKED. */
    public void revokeFamily(TokenFamily family, RefreshTokenRepository repository) {
        repository.findByFamily(family).stream()
                .filter(t -> !t.isRevoked())
                .forEach(t -> {
                    t.revoke(RevokedReason.FAMILY_REVOKED);
                    repository.save(t);
                });
    }

    public void validateOwnership(RefreshToken token, UserId userId) {
        if (!token.belongsTo(userId)) {
            throw new TokenOwnershipException(userId.value());
        }
    }
}
