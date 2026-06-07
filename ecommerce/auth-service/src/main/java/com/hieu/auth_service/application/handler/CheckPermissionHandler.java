package com.hieu.auth_service.application.handler;

import com.hieu.auth_service.application.common.QueryHandler;
import com.hieu.auth_service.application.query.CheckPermissionQuery;
import com.hieu.auth_service.domain.models.permission.vo.PermissionId;
import com.hieu.auth_service.domain.models.user.vo.UserId;
import com.hieu.auth_service.domain.repositories.PermissionRepository;
import com.hieu.auth_service.domain.repositories.RoleRepository;
import com.hieu.auth_service.domain.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Evaluates {@link CheckPermissionQuery} — returns {@code true} iff the user has the
 * named permission through any of their roles.
 *
 * <p>Two DB round-trips worst case: fetch user-roles, then fetch role-permissions +
 * permission name lookup in a single {@code findByIdIn}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckPermissionHandler implements QueryHandler<CheckPermissionQuery, Boolean> {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    @Override
    public Boolean handle(CheckPermissionQuery query) {
        return userRepository.findByIdWithRoles(UserId.of(query.userId()))
                .map(user -> hasPermission(user.getRoles(), query.permissionName()))
                .orElse(false);
    }

    private boolean hasPermission(Set<com.hieu.auth_service.domain.models.role.vo.RoleId> roleIds,
                                  String targetPermission) {
        if (ObjectUtils.isEmpty(roleIds)) return false;

        Set<PermissionId> permissionIds = roleRepository.findByIdIn(roleIds).stream()
                .flatMap(role -> role.getPermissions().stream())
                .collect(Collectors.toSet());

        if (permissionIds.isEmpty()) return false;

        return permissionRepository.findByIdIn(permissionIds).stream()
                .anyMatch(p -> p.getName().value().equals(targetPermission));
    }
}
