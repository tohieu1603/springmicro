package com.hieu.auth_service.application.handler;

import com.hieu.auth_service.application.common.QueryHandler;
import com.hieu.auth_service.application.query.CheckRoleQuery;
import com.hieu.auth_service.domain.models.user.vo.UserId;
import com.hieu.auth_service.domain.repositories.RoleRepository;
import com.hieu.auth_service.domain.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

/**
 * Evaluates {@link CheckRoleQuery} — returns {@code true} iff the user has the role.
 *
 * <p>Delegates heavy lifting to the repositories' {@code findByIdIn} methods so the query
 * stays O(roles-of-user) instead of scanning the full role registry.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckRoleHandler implements QueryHandler<CheckRoleQuery, Boolean> {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public Boolean handle(CheckRoleQuery query) {
        return userRepository.findByIdWithRoles(UserId.of(query.userId()))
                .map(user -> {
                    var roleIds = user.getRoles();
                    if (ObjectUtils.isEmpty(roleIds)) return false;
                    return roleRepository.findByIdIn(roleIds).stream()
                            .anyMatch(r -> r.getName().value().equals(query.roleName()));
                })
                .orElse(false);
    }
}
