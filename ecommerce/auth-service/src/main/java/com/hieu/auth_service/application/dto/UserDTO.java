package com.hieu.auth_service.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Set;

/**
 * Read model of a {@code User} aggregate.
 *
 * <p>Uses {@link String} for {@code id} because the domain uses UUID-backed {@code UserId};
 * previous {@code Long} typing was a bug that silently truncated values. Records
 * guarantee immutability and produce readable JSON.
 *
 * @param id                    stable user UUID as string
 * @param username              unique username
 * @param email                 primary email address
 * @param firstName             given name
 * @param lastName              family name
 * @param enabled               account-enabled flag
 * @param accountNonExpired     negated account-expired flag
 * @param accountNonLocked      negated account-locked flag
 * @param credentialsNonExpired negated credentials-expired flag
 * @param roles                 unique role names assigned to the user
 * @param permissions           unique permission names effectively granted
 * @param createdAt             creation timestamp
 * @param updatedAt             last-modified timestamp
 * @param lastLogin             last successful login, null if never
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserDTO(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        boolean accountNonExpired,
        boolean accountNonLocked,
        boolean credentialsNonExpired,
        Set<String> roles,
        Set<String> permissions,
        Instant createdAt,
        Instant updatedAt,
        Instant lastLogin
) {
}
