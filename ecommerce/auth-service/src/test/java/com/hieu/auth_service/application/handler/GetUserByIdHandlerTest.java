package com.hieu.auth_service.application.handler;

import com.hieu.auth_service.application.dto.UserDTO;
import com.hieu.auth_service.application.mapper.UserDtoMapper;
import com.hieu.auth_service.application.query.GetUserByIdQuery;
import com.hieu.auth_service.domain.models.permission.Permission;
import com.hieu.auth_service.domain.models.role.Role;
import com.hieu.auth_service.domain.models.role.vo.RoleName;
import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.exceptions.UserNotFoundException;
import com.hieu.auth_service.domain.models.user.vo.Email;
import com.hieu.auth_service.domain.models.user.vo.Password;
import com.hieu.auth_service.domain.models.user.vo.PersonName;
import com.hieu.auth_service.domain.models.user.vo.UserId;
import com.hieu.auth_service.domain.models.user.vo.Username;
import com.hieu.auth_service.domain.repositories.PermissionRepository;
import com.hieu.auth_service.domain.repositories.RoleRepository;
import com.hieu.auth_service.domain.repositories.UserRepository;
import com.hieu.auth_service.testsupport.FakePasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link GetUserByIdHandler} — user lookup with roles, effective-permission
 * resolution, and the read-model mapping. All collaborators mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetUserByIdHandler (unit)")
class GetUserByIdHandlerTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private UserDtoMapper userDtoMapper;

    private GetUserByIdHandler handler;
    private User user;

    @BeforeEach
    void setUp() {
        handler = new GetUserByIdHandler(userRepository, roleRepository, permissionRepository, userDtoMapper);
        user = User.register(Username.of("alice"), Email.of("alice@example.com"),
                Password.createRaw("password1"), PersonName.of("Alice", "Nguyen"), new FakePasswordEncoder());
    }

    @Test
    @DisplayName("no role permissions → permission repo is not queried, DTO mapped with empty permissions")
    void getUser_noPermissionsSkipsPermissionRepo() {
        UserDTO dto = org.mockito.Mockito.mock(UserDTO.class);
        Role role = Role.create(RoleName.of("ROLE_USER"), "user"); // no permissions granted
        when(userRepository.findByIdWithRoles(UserId.of(user.getId().value()))).thenReturn(Optional.of(user));
        when(roleRepository.findByIdIn(anySet())).thenReturn(List.of(role));
        when(userDtoMapper.toDto(any(User.class), anyCollection(), anyCollection())).thenReturn(dto);

        UserDTO result = handler.handle(new GetUserByIdQuery(user.getId().value()));

        assertThat(result).isSameAs(dto);
        verify(permissionRepository, never()).findByIdIn(any());
    }

    @Test
    @DisplayName("role with permissions → permission repo queried with the union of permission ids")
    void getUser_resolvesEffectivePermissions() {
        UserDTO dto = org.mockito.Mockito.mock(UserDTO.class);
        Permission readPerm = Permission.create("order", "read", "read orders");
        Role role = Role.create(RoleName.of("ROLE_USER"), "user");
        role.grantPermission(readPerm.getId());

        when(userRepository.findByIdWithRoles(UserId.of(user.getId().value()))).thenReturn(Optional.of(user));
        when(roleRepository.findByIdIn(anySet())).thenReturn(List.of(role));
        when(permissionRepository.findByIdIn(Set.of(readPerm.getId()))).thenReturn(List.of(readPerm));
        when(userDtoMapper.toDto(any(User.class), anyCollection(), anyCollection())).thenReturn(dto);

        UserDTO result = handler.handle(new GetUserByIdQuery(user.getId().value()));

        assertThat(result).isSameAs(dto);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Permission>> permsCaptor = ArgumentCaptor.forClass(List.class);
        verify(userDtoMapper).toDto(any(User.class), anyCollection(), permsCaptor.capture());
        assertThat(permsCaptor.getValue()).containsExactly(readPerm);
    }

    @Test
    @DisplayName("unknown user → UserNotFoundException, no role/permission lookups")
    void getUser_userNotFound() {
        UserId missing = UserId.generate();
        when(userRepository.findByIdWithRoles(UserId.of(missing.value()))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new GetUserByIdQuery(missing.value())))
                .isInstanceOf(UserNotFoundException.class);

        verify(roleRepository, never()).findByIdIn(any());
        verify(permissionRepository, never()).findByIdIn(any());
    }
}
