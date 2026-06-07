package com.hieu.auth_service.application.handler;

import com.hieu.auth_service.application.command.UpdateEmailCommand;
import com.hieu.auth_service.application.dto.UserDTO;
import com.hieu.auth_service.application.mapper.UserDtoMapper;
import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.exceptions.UserAlreadyExistsException;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link UpdateEmailHandler} — email uniqueness guard, aggregate mutation,
 * and the read-model mapping on success. All collaborators are mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateEmailHandler (unit)")
class UpdateEmailHandlerTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserDtoMapper userDtoMapper;

    private UpdateEmailHandler handler;
    private User user;

    @BeforeEach
    void setUp() {
        handler = new UpdateEmailHandler(userRepository, roleRepository, userDtoMapper);
        user = User.register(Username.of("alice"), Email.of("alice@example.com"),
                Password.createRaw("password1"), PersonName.of("Alice", "Nguyen"), new FakePasswordEncoder());
    }

    @Test
    @DisplayName("changes the email when the new address is free, saves, and maps the DTO")
    void updateEmail_happyPath() {
        UserDTO dto = org.mockito.Mockito.mock(UserDTO.class);
        when(userRepository.findById(UserId.of(user.getId().value()))).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail(Email.of("alice.new@example.com"))).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roleRepository.findByIdIn(any())).thenReturn(List.of());
        when(userDtoMapper.toDto(any(User.class), anyCollection())).thenReturn(dto);

        UserDTO result = handler.handle(new UpdateEmailCommand(user.getId().value(), "alice.new@example.com"));

        assertThat(result).isSameAs(dto);
        assertThat(user.getEmail()).isEqualTo(Email.of("alice.new@example.com"));
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("unknown user → UserNotFoundException, nothing persisted")
    void updateEmail_userNotFound() {
        UserId missing = UserId.generate();
        when(userRepository.findById(UserId.of(missing.value()))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new UpdateEmailCommand(missing.value(), "x@example.com")))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("email already taken by another user → UserAlreadyExistsException")
    void updateEmail_duplicateEmail() {
        when(userRepository.findById(UserId.of(user.getId().value()))).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail(Email.of("taken@example.com"))).thenReturn(true);

        assertThatThrownBy(() -> handler.handle(new UpdateEmailCommand(user.getId().value(), "taken@example.com")))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("taken@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("setting the same email skips the uniqueness check and is a no-op change, still maps DTO")
    void updateEmail_sameEmailSkipsUniquenessCheck() {
        UserDTO dto = org.mockito.Mockito.mock(UserDTO.class);
        when(userRepository.findById(UserId.of(user.getId().value()))).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roleRepository.findByIdIn(any())).thenReturn(List.of());
        when(userDtoMapper.toDto(any(User.class), anyCollection())).thenReturn(dto);
        // Same normalised email → existsByEmail must NOT be consulted.
        lenient().when(userRepository.existsByEmail(any())).thenReturn(true);

        UserDTO result = handler.handle(new UpdateEmailCommand(user.getId().value(), "ALICE@example.com"));

        assertThat(result).isSameAs(dto);
        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository).save(user);
    }
}
