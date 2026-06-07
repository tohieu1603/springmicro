package com.hieu.auth_service.application.handler;

import com.hieu.auth_service.application.common.QueryHandler;
import com.hieu.auth_service.application.dto.UserDTO;
import com.hieu.auth_service.application.mapper.UserDtoMapper;
import com.hieu.auth_service.application.query.GetUserByUsernameQuery;
import com.hieu.auth_service.domain.models.user.exceptions.UserNotFoundException;
import com.hieu.auth_service.domain.models.user.vo.Username;
import com.hieu.auth_service.domain.repositories.RoleRepository;
import com.hieu.auth_service.domain.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lightweight lookup by username. Skips permission resolution since this query is typically
 * used for admin "who is this" look-ups, not authorization decisions.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetUserByUsernameHandler implements QueryHandler<GetUserByUsernameQuery, UserDTO> {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserDtoMapper userDtoMapper;

    @Override
    public UserDTO handle(GetUserByUsernameQuery query) {
        var user = userRepository.findByUsernameWithRoles(Username.of(query.username()))
                .orElseThrow(() -> new UserNotFoundException(query.username()));
        return userDtoMapper.toDto(user, roleRepository.findByIdIn(user.getRoles()));
    }
}
