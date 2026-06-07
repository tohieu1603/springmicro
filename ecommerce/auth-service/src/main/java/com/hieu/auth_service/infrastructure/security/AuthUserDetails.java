package com.hieu.auth_service.infrastructure.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.vo.AccountStatus;

/**
 * Custom {@link UserDetails} returned by {@link   }.
 *
 * <p>Carries fields that Spring's default {@code User} builder can't model:
 * <ul>
 *   <li>{@link #userId} — the stable UUID string id from the domain aggregate; easier to use
 *       than the username for permission lookups</li>
 *   <li>{@link #tokenVersion} — compared against the JWT's {@code tokenVersion} claim so
 *       tokens minted before a password change are rejected</li>
 * </ul>
 *
 * <p>The class is deliberately {@code final} + immutable (defensive copies for authorities).
 * Principal objects are serialised to SecurityContext and sometimes cached, so mutability
 * would invite subtle bugs.
 */
public final class AuthUserDetails implements UserDetails {

    private final String userId;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final int tokenVersion;
    private final Set<GrantedAuthority> authorities;
    private final boolean enabled;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;

    private AuthUserDetails(String userId, String username, String email, String passwordHash,
                            int tokenVersion, Collection<? extends GrantedAuthority> authorities,
                            AccountStatus status) {
        this.userId = Objects.requireNonNull(userId);
        this.username = Objects.requireNonNull(username);
        this.email = email;
        this.passwordHash = passwordHash;
        this.tokenVersion = tokenVersion;
        this.authorities = Set.copyOf(authorities);
        this.enabled = status.enabled();
        this.accountNonExpired = status.accountNonExpired();
        this.accountNonLocked = status.accountNonLocked();
        this.credentialsNonExpired = status.credentialsNonExpired();
    }

    /**
     * Builds a principal from a domain {@link User} plus resolved role + permission names.
     *
     * @param user                  source aggregate
     * @param roleNames             role names ({@code ROLE_*}) for authorities
     * @param permissionNames       permission names for fine-grained {@code @PreAuthorize("hasAuthority(...)")}
     * @return populated principal
     */
    public static AuthUserDetails from(User user, Set<String> roleNames, Set<String> permissionNames) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        roleNames.forEach(r -> authorities.add(new SimpleGrantedAuthority(r)));
        permissionNames.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));

        return new AuthUserDetails(
                user.getId().value(),
                user.getUsername().value(),
                user.getEmail().value(),
                user.getPassword().value(),
                user.getTokenVersion(),
                authorities,
                user.getAccountStatus());
    }

    // ── Domain-side accessors (used by filters + controllers) ──

    public String userId() { return userId; }
    public int tokenVersion() { return tokenVersion; }
    public String email() { return email; }

    // ── UserDetails contract ──

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return username; }
    @Override public boolean isEnabled()               { return enabled; }
    @Override public boolean isAccountNonExpired()     { return accountNonExpired; }
    @Override public boolean isAccountNonLocked()      { return accountNonLocked; }
    @Override public boolean isCredentialsNonExpired() { return credentialsNonExpired; }

    @Override
    public boolean equals(Object o) {
        return o instanceof AuthUserDetails other && userId.equals(other.userId);
    }

    @Override 
    public int hashCode() { return userId.hashCode(); }
}
