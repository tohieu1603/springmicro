package com.hieu.auth_service.infrastructure.persistence.mapper;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

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

/**
 * Pure unit tests for the hand-written {@link UserJpaMapper} ACL: domain {@code User} <-> JPA
 * entity field mapping, value-object packing/unpacking, googleSub null handling, role-id
 * collection translation, the in-place {@code updateJpaEntity}, and null guards.
 */
class UserJpaMapperTest {

    private final UserJpaMapper mapper = new UserJpaMapper();

    private static final String USER_ID = "22222222-2222-2222-2222-222222222222";
    private static final String ROLE_ID = "33333333-3333-3333-3333-333333333333";

    private User domainUser(GoogleSub googleSub) {
        Instant created = Instant.parse("2024-01-01T00:00:00Z");
        Instant updated = Instant.parse("2024-02-02T00:00:00Z");
        Instant lastLogin = Instant.parse("2024-03-03T00:00:00Z");
        AccountStatus status = AccountStatus.of(true, true, false, true, lastLogin);
        return User.reconstitute(
                UserId.of(USER_ID),
                Username.of("bob"),
                Email.of("bob@example.com"),
                Password.createEncoded("$2a$10$encodedencodedencodedo"),
                PersonName.of("Bob", "Jones"),
                status,
                Set.of(RoleId.of(ROLE_ID)),
                5,
                googleSub,
                created,
                updated);
    }

    private RoleJpaEntity roleEntity() {
        return RoleJpaEntity.builder().id(ROLE_ID).name("ROLE_USER").build();
    }

    @Test
    void toJpaEntity_mapsAllFields() {
        User user = domainUser(GoogleSub.of("google-sub-123"));

        UserJpaEntity entity = mapper.toJpaEntity(user, Set.of(roleEntity()), true);

        assertThat(entity.getId()).isEqualTo(USER_ID);
        assertThat(entity.getUsername()).isEqualTo("bob");
        assertThat(entity.getEmail()).isEqualTo("bob@example.com");
        assertThat(entity.getPassword()).isEqualTo("$2a$10$encodedencodedencodedo");
        assertThat(entity.getFirstName()).isEqualTo("Bob");
        assertThat(entity.getLastName()).isEqualTo("Jones");
        assertThat(entity.isEnabled()).isTrue();
        assertThat(entity.isAccountNonExpired()).isTrue();
        assertThat(entity.isAccountNonLocked()).isFalse();
        assertThat(entity.isCredentialsNonExpired()).isTrue();
        assertThat(entity.getLastLogin()).isEqualTo(Instant.parse("2024-03-03T00:00:00Z"));
        assertThat(entity.getTokenVersion()).isEqualTo(5);
        assertThat(entity.getGoogleSub()).isEqualTo("google-sub-123");
        assertThat(entity.getRoles()).hasSize(1);
        assertThat(entity.getCreatedAt()).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
        assertThat(entity.getUpdatedAt()).isEqualTo(Instant.parse("2024-02-02T00:00:00Z"));
        assertThat(entity.isNew()).isTrue();
    }

    @Test
    void toJpaEntity_nullGoogleSubMapsToNull() {
        UserJpaEntity entity = mapper.toJpaEntity(domainUser(null), Set.of(roleEntity()), false);

        assertThat(entity.getGoogleSub()).isNull();
        assertThat(entity.isNew()).isFalse();
    }

    @Test
    void toDomain_reconstitutesAggregate() {
        UserJpaEntity entity = mapper.toJpaEntity(domainUser(GoogleSub.of("sub-9")), Set.of(roleEntity()), true);

        User domain = mapper.toDomain(entity);

        assertThat(domain.getId().value()).isEqualTo(USER_ID);
        assertThat(domain.getUsername().value()).isEqualTo("bob");
        assertThat(domain.getEmail().value()).isEqualTo("bob@example.com");
        assertThat(domain.getPersonName().firstName()).isEqualTo("Bob");
        assertThat(domain.getPersonName().lastName()).isEqualTo("Jones");
        assertThat(domain.getTokenVersion()).isEqualTo(5);
        assertThat(domain.getGoogleSub().value()).isEqualTo("sub-9");
        assertThat(domain.getRoles()).extracting(RoleId::value).containsExactly(ROLE_ID);
        assertThat(domain.getAccountStatus().accountNonLocked()).isFalse();
        assertThat(domain.getAccountStatus().lastLogin()).isEqualTo(Instant.parse("2024-03-03T00:00:00Z"));
    }

    @Test
    void roundTrip_preservesIdentityAndState() {
        User original = domainUser(GoogleSub.of("rt-sub"));

        User rebuilt = mapper.toDomain(mapper.toJpaEntity(original, Set.of(roleEntity()), true));

        assertThat(rebuilt.getId()).isEqualTo(original.getId());
        assertThat(rebuilt.getUsername()).isEqualTo(original.getUsername());
        assertThat(rebuilt.getEmail()).isEqualTo(original.getEmail());
        assertThat(rebuilt.getTokenVersion()).isEqualTo(original.getTokenVersion());
        assertThat(rebuilt.getGoogleSub()).isEqualTo(original.getGoogleSub());
    }

    @Test
    void toDomain_nullTokenVersionDefaultsToOne() {
        UserJpaEntity entity = UserJpaEntity.builder()
                .id(USER_ID)
                .username("carol")
                .email("carol@example.com")
                .password("$2a$10$x")
                .firstName("Carol")
                .lastName("King")
                .enabled(true).accountNonExpired(true).accountNonLocked(true).credentialsNonExpired(true)
                .tokenVersion(null)
                .roles(Set.of())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isNew(false)
                .build();

        User domain = mapper.toDomain(entity);

        assertThat(domain.getTokenVersion()).isEqualTo(1);
        assertThat(domain.getGoogleSub()).isNull();
    }

    @Test
    void updateJpaEntity_appliesMutationsInPlace() {
        UserJpaEntity managed = mapper.toJpaEntity(domainUser(null), Set.of(roleEntity()), false);
        User changed = domainUser(GoogleSub.of("new-sub"));

        mapper.updateJpaEntity(changed, managed, Set.of());

        assertThat(managed.getGoogleSub()).isEqualTo("new-sub");
        assertThat(managed.getRoles()).isEmpty();
        assertThat(managed.getTokenVersion()).isEqualTo(5);
        assertThat(managed.getUpdatedAt()).isEqualTo(Instant.parse("2024-02-02T00:00:00Z"));
    }

    @Test
    void nullInputs_returnNull() {
        assertThat(mapper.toJpaEntity(null, Set.of(), true)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }
}
