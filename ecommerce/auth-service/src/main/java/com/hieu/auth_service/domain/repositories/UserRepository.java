package com.hieu.auth_service.domain.repositories;


import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.vo.Email;
import com.hieu.auth_service.domain.models.user.vo.GoogleSub;
import com.hieu.auth_service.domain.models.user.vo.UserId;
import com.hieu.auth_service.domain.models.user.vo.Username;

/**
 * Defines the contracts for User persistence without exposing implementation details
 */
public interface UserRepository {
    /**
     * Save a user (insert or update)
     */
    User save(User user);

    /**
     * Find user by ID
     */
    Optional<User> findById(UserId userId);

    /**
     * Find user by username
     */
    Optional<User> findByUsername(Username username);

    /**
     * Find user by email
     */
    Optional<User> findByEmail(Email email);

    /**
     * Find user by Google {@code sub} claim. Preferred over email when
     * linking accounts because sub is immutable across email changes.
     */
    Optional<User> findByGoogleSub(GoogleSub googleSub);

    /**
     * Returns true if the candidate username is already taken — used by the
     * Google flow to pick a free derived username (alice, alice1, alice2…).
     */
    default boolean isUsernameTaken(Username username) {
        return findByUsername(username).isPresent();
    }

    /**
     * Check if username exists
     */
    boolean existsByUsername(Username username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(Email email);

    /**
     * Delete user
     */
    void delete(User user);

    /**
     * Find user by ID with roles loaded (for authorization)
     */
    Optional<User> findByIdWithRoles(UserId userId);

    /**
     * Find user by username with roles loaded (for authentication)
     */
    Optional<User> findByUsernameWithRoles(Username username);

    /**
     * Keyset pagination on {@code (createdAt DESC, id DESC)}.
     * First page: pass {@code null} for both cursor fields.
     *
     * @param cursorCreatedAt timestamp anchor from previous page, null for first page
     * @param cursorId        id anchor for tie-breaking same-timestamp rows
     * @param limit           max rows to return (caller-clamped)
     * @return up to {@code limit} users
     */
    List<User> findAfterCursor(Instant cursorCreatedAt, String cursorId, int limit);
}
