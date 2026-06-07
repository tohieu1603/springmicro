package com.hieu.auth_service.application.handler;

import com.hieu.auth_service.application.command.AssignRoleCommand;
import com.hieu.auth_service.domain.models.role.Role;
import com.hieu.auth_service.domain.models.role.vo.RoleName;
import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.exceptions.UserNotFoundException;
import com.hieu.auth_service.domain.models.user.vo.Email;
import com.hieu.auth_service.domain.models.user.vo.Password;
import com.hieu.auth_service.domain.models.user.vo.PersonName;
import com.hieu.auth_service.domain.models.user.vo.UserId;
import com.hieu.auth_service.domain.models.user.vo.Username;
import com.hieu.auth_service.domain.repositories.RoleRepository;
import com.hieu.auth_service.domain.repositories.UserRepository;
import com.hieu.auth_service.testsupport.FakePasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link AssignRoleHandler} — user/role resolution and the assignment write.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssignRoleHandler (unit)")
class AssignRoleHandlerTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;

    private AssignRoleHandler handler;
    private User user;

    @BeforeEach
    void setUp() {
        handler = new AssignRoleHandler(userRepository, roleRepository);
        user = User.register(Username.of("alice"), Email.of("alice@example.com"),
                Password.createRaw("password1"), PersonName.of("Alice", "Nguyen"), new FakePasswordEncoder());
    }

    @Test
    @DisplayName("assigns the resolved role to the user and saves")
    void assignRole_happyPath() {
        Role admin = Role.create(RoleName.of("ROLE_ADMIN"), "admins");
        when(userRepository.findById(UserId.of(user.getId().value()))).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.of("ROLE_ADMIN"))).thenReturn(Optional.of(admin));

        handler.handle(new AssignRoleCommand(user.getId().value(), "ROLE_ADMIN"));

        assertThat(user.hasRole(admin.getId())).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("unknown user → UserNotFoundException, no role lookup, no save")
    void assignRole_userNotFound() {
        UserId missing = UserId.generate();
        when(userRepository.findById(UserId.of(missing.value()))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new AssignRoleCommand(missing.value(), "ROLE_ADMIN")))
                .isInstanceOf(UserNotFoundException.class);

        verify(roleRepository, never()).findByName(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("unknown role → IllegalArgumentException, user not saved")
    void assignRole_roleNotFound() {
        when(userRepository.findById(UserId.of(user.getId().value()))).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.of("ROLE_GHOST"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new AssignRoleCommand(user.getId().value(), "ROLE_GHOST")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Role not found");

        verify(userRepository, never()).save(any());
    }
}
