package com.hieu.auth_service.domain.repositories;

import com.hieu.auth_service.domain.models.refreshtoken.RefreshToken;
import com.hieu.auth_service.domain.models.refreshtoken.vo.TokenFamily;
import com.hieu.auth_service.domain.models.refreshtoken.vo.TokenId;
import com.hieu.auth_service.domain.models.refreshtoken.vo.TokenValue;
import com.hieu.auth_service.domain.models.user.vo.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Domain Repository Interface for RefreshToken Aggregate
 */
public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findById(TokenId tokenId);

    Optional<RefreshToken> findByTokenValue(TokenValue tokenValue);

    /**
     * Same as {@link #findByTokenValue} but acquires a pessimistic write lock on the
     * row. Use this in the refresh-token rotation flow to serialize concurrent
     * refreshes of the same token — without locking, two parallel requests both read
     * a still-valid token and each issue a new access token (double session).
     */
    Optional<RefreshToken> findByTokenValueForUpdate(TokenValue tokenValue);

    List<RefreshToken> findByUserId(UserId userId);

    List<RefreshToken> findValidTokensByUserId(UserId userId);

    /** Find all tokens in a token family (for reuse detection / family revocation) */
    List<RefreshToken> findByFamily(TokenFamily family);

    void delete(RefreshToken token);

    void deleteByUserId(UserId userId);

    int deleteExpiredTokens();

    void revokeAllTokensForUser(UserId userId);
}
