package com.hieu.auth_service.application.handler;

import com.hieu.auth_service.application.command.UpdateEmailCommand;
import com.hieu.auth_service.application.common.CommandHandler;
import com.hieu.auth_service.application.dto.UserDTO;
import com.hieu.auth_service.application.mapper.UserDtoMapper;
import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.exceptions.UserAlreadyExistsException;
import com.hieu.auth_service.domain.models.user.exceptions.UserNotFoundException;
import com.hieu.auth_service.domain.models.user.vo.Email;
import com.hieu.auth_service.domain.models.user.vo.UserId;
import com.hieu.auth_service.domain.repositories.RoleRepository;
import com.hieu.auth_service.domain.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles {@link UpdateEmailCommand}. Enforces email uniqueness before mutating the aggregate.
 */
@Service
@RequiredArgsConstructor
public class UpdateEmailHandler implements CommandHandler<UpdateEmailCommand, UserDTO> {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserDtoMapper userDtoMapper;

    @Override
    @Transactional
    public UserDTO handle(UpdateEmailCommand command) {
        User user = userRepository.findById(UserId.of(command.userId()))
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        Email newEmail = Email.of(command.newEmail());
        if (!user.getEmail().equals(newEmail) && userRepository.existsByEmail(newEmail)) {
            throw new UserAlreadyExistsException("email=" + command.newEmail());
        }

        user.updateEmail(newEmail);
        User saved = userRepository.save(user);
        return userDtoMapper.toDto(saved, roleRepository.findByIdIn(saved.getRoles()));
    }
}
