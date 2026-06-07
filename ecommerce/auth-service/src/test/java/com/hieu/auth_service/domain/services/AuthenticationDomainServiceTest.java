package com.hieu.auth_service.domain.services;

import com.hieu.auth_service.domain.models.permission.Permission;
import com.hieu.auth_service.domain.models.permission.vo.PermissionId;
import com.hieu.auth_service.domain.models.role.Role;
import com.hieu.auth_service.domain.models.role.vo.RoleName;
import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.vo.Email;
import com.hieu.auth_service.domain.models.user.vo.Password;
import com.hieu.auth_service.domain.models.user.vo.PersonName;
import com.hieu.auth_service.domain.models.user.vo.Username;
import com.hieu.auth_service.testsupport.FakePasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for {@link AuthenticationDomainService} — permission/role resolution across
 * the User → Role → Permission aggregates. All inputs are real domain objects (no mocks needed).
 */
@DisplayName("AuthenticationDomainService (unit)")
class AuthenticationDomainServiceTest {

    private final AuthenticationDomainService service = new AuthenticationDomainService();

    private User user;
    private Role adminRole;
    private Permission userReadPermission;

    @BeforeEach
    void setup() {
        user = User.register(
                Username.of("dave"),
                Email.of("dave@example.com"),
                Password.createRaw("password1"),
                PersonName.of("Dave", "Pham"),
                new FakePasswordEncoder());

        // Grant the role a permission id, and build the matching system Permission with that id.
        PermissionId permId = PermissionId.generate();
        userReadPermission = Permission.reconstitute(
                permId, "USER", "READ", "Read users", Instant.now(), Instant.now());

        adminRole = Role.create(RoleName.of("ADMIN"), "Administrators");
        adminRole.grantPermission(permId);
    }

    @Test
    @DisplayName("hasPermission() resolves a granted permission by name and rejects others")
    void hasPermission() {
        assertThat(service.hasPermission(user, List.of(adminRole), List.of(userReadPermission), "USER_READ"))
                .isTrue();
        assertThat(service.hasPermission(user, List.of(adminRole), List.of(userReadPermission), "USER_WRITE"))
                .isFalse();
    }

    @Test
    @DisplayName("hasRole() matches the normalised role name")
    void hasRole() {
        assertThat(service.hasRole(user, List.of(adminRole), "ROLE_ADMIN")).isTrue();
        assertThat(service.hasRole(user, List.of(adminRole), "ROLE_USER")).isFalse();
    }

    @Test
    @DisplayName("getPermissionNames() returns the effective permission names")
    void getPermissionNames() {
        assertThat(service.getPermissionNames(user, List.of(adminRole), List.of(userReadPermission)))
                .containsExactly("USER_READ");
    }

    @Test
    @DisplayName("canAccessResource() honours resource+action granted via the role")
    void canAccessResource() {
        assertThat(service.canAccessResource(user, List.of(adminRole), List.of(userReadPermission), "USER", "READ"))
                .isTrue();
        assertThat(service.canAccessResource(user, List.of(adminRole), List.of(userReadPermission), "USER", "DELETE"))
                .isFalse();
        assertThat(service.canAccessResource(user, List.of(adminRole), List.of(userReadPermission), "ORDER", "READ"))
                .isFalse();
    }

    @Test
    @DisplayName("a user with no roles has no permissions")
    void noRoles_noPermissions() {
        assertThat(service.hasPermission(user, List.of(), List.of(userReadPermission), "USER_READ")).isFalse();
        assertThat(service.getPermissionNames(user, List.of(), List.of(userReadPermission))).isEmpty();
    }
}
