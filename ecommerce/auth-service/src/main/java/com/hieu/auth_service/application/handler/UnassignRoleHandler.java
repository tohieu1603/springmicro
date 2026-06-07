package com.hieu.auth_service.application.handler;

import com.hieu.auth_service.application.command.UnassignRoleCommand;
import com.hieu.auth_service.application.common.CommandHandler;
import com.hieu.auth_service.domain.models.role.Role;
import com.hieu.auth_service.domain.models.role.vo.RoleName;
import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.exceptions.UserNotFoundException;
import com.hieu.auth_service.domain.models.user.vo.UserId;
import com.hieu.auth_service.domain.repositories.RoleRepository;
import com.hieu.auth_service.domain.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles {@link UnassignRoleCommand}. No-ops when the user does not currently have the role.
 */
@Service
@RequiredArgsConstructor
public class UnassignRoleHandler implements CommandHandler<UnassignRoleCommand, Void> {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public Void handle(UnassignRoleCommand command) {
        User user = userRepository.findById(UserId.of(command.userId()))
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        Role role = roleRepository.findByName(RoleName.of(command.roleName()))
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + command.roleName()));

        user.unassignRole(role.getId());
        userRepository.save(user);
        return null;
    }
}
