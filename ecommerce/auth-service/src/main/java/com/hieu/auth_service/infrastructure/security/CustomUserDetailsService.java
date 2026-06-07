package com.hieu.auth_service.infrastructure.security;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hieu.auth_service.application.port.RolePermissionCachePort;
import com.hieu.auth_service.domain.models.role.Role;
import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.vo.UserId;
import com.hieu.auth_service.domain.models.user.vo.Username;
import com.hieu.auth_service.domain.repositories.PermissionRepository;
import com.hieu.auth_service.domain.repositories.RoleRepository;
import com.hieu.auth_service.domain.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Security {@link UserDetailsService} wired to the domain.
 *
 * <p>Goes through domain repositories ({@link UserRepository}/{@link RoleRepository}/
 * {@link PermissionRepository}) rather than JPA entities, keeping Spring Security
 * decoupled from persistence details.
 *
 * <p>Permission resolution is Redis-cached via {@link RolePermissionCachePort}. On each
 * request the filter calls {@link #loadUserById}; the permission names per role are read
 * from Redis first and only fall back to Postgres when the cache is cold or missing
 * (e.g. during a Redis outage). Cache invalidation is driven by Kafka events emitted
 * when a role's permissions change.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionCachePort rolePermissionCache;

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // H1: Generic message prevents username enumeration via error responses.
        User user = userRepository.findByUsernameWithRoles(Username.of(username))
                .orElseThrow(() -> new UsernameNotFoundException("Authentication failed"));
        return toUserDetails(user);
    }

    /**
     * Loads principal by stable user id (UUID) — used by the JWT filter to skip
     * the {@code by_username} index when the id is already known.
     *
     * @param userIdRaw UUID string from the JWT subject/userId claim
     * @return populated principal
     * @throws UsernameNotFoundException if no user exists with that id
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(String userIdRaw) {
        // H1: Generic message — do not leak internal user IDs in error responses.
        User user = userRepository.findByIdWithRoles(UserId.of(userIdRaw))
                .orElseThrow(() -> new UsernameNotFoundException("Authentication failed"));
        return toUserDetails(user);
    }

    /**
     * Assembles {@link AuthUserDetails} from a loaded user. Permission lookup goes
     * Redis-first, DB-fallback.
     */
    private AuthUserDetails toUserDetails(User user) {
        var roles = roleRepository.findByIdIn(user.getRoles());

        Set<String> roleNames = roles.stream()
                .map(r -> r.getName().value())
                .collect(Collectors.toSet());

        Set<String> permissionNames = new LinkedHashSet<>();
        for (Role role : roles) {
            permissionNames.addAll(resolvePermissions(role));
        }

        return AuthUserDetails.from(user, roleNames, permissionNames);
    }

    /**
     * Redis-first permission lookup for a single role.
     *
     * <p>Cache hit → return immediately. Cache miss → query Postgres, populate Redis
     * for subsequent reads. Treats {@code null} from the port as a miss (Redis down
     * or cold entry) and still serves a correct answer.
     */
    private Set<String> resolvePermissions(Role role) {
        String roleName = role.getName().value();
        Set<String> cached = rolePermissionCache.get(roleName);
        if (cached != null) return cached;

        Set<String> resolved = role.getPermissions().isEmpty()
                ? Set.of()
                : permissionRepository.findByIdIn(role.getPermissions()).stream()
                        .map(p -> p.getName().value())
                        .collect(Collectors.toSet()); 

        rolePermissionCache.put(roleName, resolved);
        return resolved;
    }
}
