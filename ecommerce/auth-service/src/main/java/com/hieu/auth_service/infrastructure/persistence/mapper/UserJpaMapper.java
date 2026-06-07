package com.hieu.auth_service.infrastructure.persistence.mapper;

import com.hieu.auth_service.domain.models.role.vo.RoleId;
import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.vo.AccountStatus;
import com.hieu.auth_service.domain.models.user.vo.Email;
import com.hieu.auth_service.domain.models.user.vo.GoogleSub;
import com.hieu.auth_service.domain.models.user.vo.Password;
import com.hieu.auth_service.domain.models.user.vo.PersonName;
import com.hieu.auth_service.domain.models.user.vo.UserId;
import com.hieu.auth_service.domain.models.user.vo.Username;
import com.hieu.auth_service.infrastructure.persistence.jpa.entities.RoleJpaEntity;
import com.hieu.auth_service.infrastructure.persistence.jpa.entities.UserJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Anti-Corruption Layer between the {@code User} aggregate and its JPA entity.
 *
 * <p>All translation lives here so the domain class can remain framework-free and the
 * JPA entity can evolve independently.
 */
@Component
public class UserJpaMapper {

    /**
     * Projects a domain {@link User} onto a persistable {@link UserJpaEntity}.
     *
     * @param user         domain aggregate
     * @param roleEntities JPA role entities already loaded in the current persistence context
     * @param isNew        true → INSERT path; false → UPDATE path (used by Persistable contract)
     * @return populated JPA entity
     */
    public UserJpaEntity toJpaEntity(User user, Set<RoleJpaEntity> roleEntities, boolean isNew) {
        if (user == null) return null;
        var s = user.getAccountStatus();
        return UserJpaEntity.builder()
                .id(user.getId().value())
                .username(user.getUsername().value())
                .email(user.getEmail().value())
                .password(user.getPassword().value())
                .firstName(user.getPersonName().firstName())
                .lastName(user.getPersonName().lastName())
                .enabled(s.enabled())
                .accountNonExpired(s.accountNonExpired())
                .accountNonLocked(s.accountNonLocked())
                .credentialsNonExpired(s.credentialsNonExpired())
                .lastLogin(s.lastLogin())
                .tokenVersion(user.getTokenVersion())
                .googleSub(user.getGoogleSub() != null ? user.getGoogleSub().value() : null)
                .roles(roleEntities)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .isNew(isNew)
                .build();
    }

    /**
     * Rebuilds a domain {@link User} aggregate from its JPA representation.
     *
     * @param entity JPA entity as loaded by Hibernate
     * @return domain aggregate, or {@code null} when the input is {@code null}
     */
    public User toDomain(UserJpaEntity entity) {
        if (entity == null) return null;

        Set<RoleId> roleIds = entity.getRoles().stream()
                .map(role -> RoleId.of(role.getId()))
                .collect(Collectors.toSet());

        AccountStatus status = AccountStatus.of(
                entity.isEnabled(),
                entity.isAccountNonExpired(),
                entity.isAccountNonLocked(),
                entity.isCredentialsNonExpired(),
                entity.getLastLogin());

        return User.reconstitute(
                UserId.of(entity.getId()),
                Username.of(entity.getUsername()),
                Email.of(entity.getEmail()),
                Password.createEncoded(entity.getPassword()),
                PersonName.of(entity.getFirstName(), entity.getLastName()),
                status,
                roleIds,
                entity.getTokenVersion() != null ? entity.getTokenVersion() : 1,
                entity.getGoogleSub() != null ? GoogleSub.of(entity.getGoogleSub()) : null,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    /**
     * Applies domain mutations onto an already-managed JPA entity.
     * Used on UPDATE paths where we want Hibernate to compute dirty-state against the row.
     *
     * @param user         source domain aggregate
     * @param entity       managed JPA entity to update in place
     * @param roleEntities resolved role entities (persistence-context-bound)
     */
    public void updateJpaEntity(User user, UserJpaEntity entity, Set<RoleJpaEntity> roleEntities) {
        var s = user.getAccountStatus();
        entity.setUsername(user.getUsername().value());
        entity.setEmail(user.getEmail().value());
        entity.setPassword(user.getPassword().value());
        entity.setFirstName(user.getPersonName().firstName());
        entity.setLastName(user.getPersonName().lastName());
        entity.setEnabled(s.enabled());
        entity.setAccountNonExpired(s.accountNonExpired());
        entity.setAccountNonLocked(s.accountNonLocked());
        entity.setCredentialsNonExpired(s.credentialsNonExpired());
        entity.setLastLogin(s.lastLogin());
        entity.setTokenVersion(user.getTokenVersion());
        entity.setGoogleSub(user.getGoogleSub() != null ? user.getGoogleSub().value() : null);
        entity.setRoles(roleEntities);
        entity.setUpdatedAt(user.getUpdatedAt());
    }
}
