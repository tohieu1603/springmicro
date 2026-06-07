package com.hieu.auth_service.domain.models.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.hieu.auth_service.domain.events.DomainEvent;
import com.hieu.auth_service.domain.models.role.vo.RoleId;
import com.hieu.auth_service.domain.models.user.events.AccountStatusChangedEvent;
import com.hieu.auth_service.domain.models.user.events.EmailChangedEvent;
import com.hieu.auth_service.domain.models.user.events.OAuthProviderLinkedEvent;
import com.hieu.auth_service.domain.models.user.events.PasswordChangedEvent;
import com.hieu.auth_service.domain.models.user.events.RoleAssignedEvent;
import com.hieu.auth_service.domain.models.user.events.UserCreatedEvent;
import com.hieu.auth_service.domain.models.user.events.UserLoggedInEvent;
import com.hieu.auth_service.domain.models.user.exceptions.AccountNotUsableException;
import com.hieu.auth_service.domain.models.user.exceptions.InvalidCredentialsException;
import com.hieu.auth_service.domain.models.user.exceptions.OAuthAccountAlreadyLinkedException;
import com.hieu.auth_service.domain.models.user.vo.Email;
import com.hieu.auth_service.domain.models.user.vo.GoogleSub;
import com.hieu.auth_service.domain.models.user.vo.Password;
import com.hieu.auth_service.domain.models.user.vo.PersonName;
import com.hieu.auth_service.domain.models.user.vo.Username;
import com.hieu.auth_service.domain.services.PasswordEncoderPort;
import com.hieu.auth_service.testsupport.FakePasswordEncoder;

/**
 * Pure unit tests for the {@link User} aggregate — registration, authentication, credential
 * changes, account-status transitions, role assignment, and Google account linking.
 */
@DisplayName("User aggregate (unit)")
class UserTest {

    private final PasswordEncoderPort encoder = new FakePasswordEncoder();

    private User newUser() {
        return User.register(
                Username.of("alice"),
                Email.of("alice@example.com"),
                Password.createRaw("password1"),
                PersonName.of("Alice", "Nguyen"),
                encoder);
    }

    private static long countEvents(User u, Class<? extends DomainEvent> type) {
        return u.peekDomainEvents().stream().filter(type::isInstance).count();
    }

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("hashes the raw password and starts active with tokenVersion=1")
        void register_hashesPasswordAndActivates() {
            User u = newUser();

            assertThat(u.getPassword().value()).isEqualTo("ENC(password1)");
            assertThat(u.getPassword().encoded()).isTrue();
            assertThat(u.isActive()).isTrue();
            assertThat(u.getTokenVersion()).isEqualTo(1);
            assertThat(u.getRoles()).isEmpty();
            assertThat(countEvents(u, UserCreatedEvent.class)).isEqualTo(1);
        }

        @Test
        @DisplayName("rejects an already-encoded password")
        void register_rejectsEncodedPassword() {
            assertThatThrownBy(() -> User.register(
                    Username.of("bob"),
                    Email.of("bob@example.com"),
                    Password.createEncoded("ENC(precomputed)"),
                    PersonName.of("Bob", "Tran"),
                    encoder))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("authenticate()")
    class Authenticate {

        @Test
        @DisplayName("returns true for the correct password and records the login")
        void authenticate_correctPassword() {
            User u = newUser();

            boolean ok = u.authenticate(Password.createRaw("password1"), encoder);

            assertThat(ok).isTrue();
            assertThat(u.getAccountStatus().lastLogin()).isNotNull();
            assertThat(countEvents(u, UserLoggedInEvent.class)).isEqualTo(1);
        }

        @Test
        @DisplayName("returns false for a wrong password and records no login")
        void authenticate_wrongPassword() {
            User u = newUser();

            boolean ok = u.authenticate(Password.createRaw("wrongpass1"), encoder);

            assertThat(ok).isFalse();
            assertThat(u.getAccountStatus().lastLogin()).isNull();
            assertThat(countEvents(u, UserLoggedInEvent.class)).isZero();
        }

        @Test
        @DisplayName("throws AccountNotUsableException(DISABLED) when the account is disabled")
        void authenticate_disabledAccount() {
            User u = newUser();
            u.disable();

            assertThatThrownBy(() -> u.authenticate(Password.createRaw("password1"), encoder))
                    .isInstanceOf(AccountNotUsableException.class)
                    .extracting(e -> ((AccountNotUsableException) e).reason())
                    .isEqualTo(AccountNotUsableException.Reason.DISABLED);
        }
    }
 
    @Nested
    @DisplayName("changePassword()")
    class ChangePassword {

        @Test
        @DisplayName("re-hashes and bumps tokenVersion when the old password matches")
        void changePassword_success() {
            User u = newUser();

            u.changePassword(Password.createRaw("password1"), Password.createRaw("newpass123"), encoder);

            assertThat(u.getPassword().value()).isEqualTo("ENC(newpass123)");
            assertThat(u.getTokenVersion()).isEqualTo(2);
            assertThat(countEvents(u, PasswordChangedEvent.class)).isEqualTo(1);
        }

        @Test
        @DisplayName("throws InvalidCredentialsException when the old password is wrong")
        void changePassword_wrongOld() {
            User u = newUser();

            assertThatThrownBy(() -> u.changePassword(
                    Password.createRaw("notmypass1"), Password.createRaw("newpass123"), encoder))
                    .isInstanceOf(InvalidCredentialsException.class);
            assertThat(u.getTokenVersion()).isEqualTo(1); // unchanged
        }
    }

    @Nested
    @DisplayName("updateEmail()")
    class UpdateEmail {

        @Test
        @DisplayName("changes the email and emits EmailChangedEvent")
        void updateEmail_changes() {
            User u = newUser();

            u.updateEmail(Email.of("alice.new@example.com"));

            assertThat(u.getEmail()).isEqualTo(Email.of("alice.new@example.com"));
            assertThat(countEvents(u, EmailChangedEvent.class)).isEqualTo(1);
        }

        @Test
        @DisplayName("is a no-op when the email is unchanged")
        void updateEmail_sameValueNoop() {
            User u = newUser();

            u.updateEmail(Email.of("ALICE@example.com")); // normalised to the same value

            assertThat(countEvents(u, EmailChangedEvent.class)).isZero();
        }
    }

    @Nested
    @DisplayName("account-status transitions")
    class StatusTransitions {

        @Test
        @DisplayName("lock() blocks authentication with reason LOCKED")
        void lock() {
            User u = newUser();

            u.lock();

            assertThat(u.isActive()).isFalse();
            assertThat(countEvents(u, AccountStatusChangedEvent.class)).isEqualTo(1);
            assertThatThrownBy(u::ensureAuthenticatable)
                    .isInstanceOf(AccountNotUsableException.class)
                    .extracting(e -> ((AccountNotUsableException) e).reason())
                    .isEqualTo(AccountNotUsableException.Reason.LOCKED);
        }

        @Test
        @DisplayName("unlock() restores authentication after a lock")
        void unlockRestores() {
            User u = newUser();
            u.lock();

            u.unlock();

            assertThat(u.isActive()).isTrue();
            assertThatCode(u::ensureAuthenticatable).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("repeating the same transition emits no extra event")
        void repeatedTransitionIdempotent() {
            User u = newUser();

            u.enable(); // already enabled — no change

            assertThat(countEvents(u, AccountStatusChangedEvent.class)).isZero();
        }
    }

    @Nested
    @DisplayName("role assignment")
    class Roles {

        @Test
        @DisplayName("assignRole() is idempotent for the same role id")
        void assignRole_idempotent() {
            User u = newUser();
            RoleId roleId = RoleId.generate();

            u.assignRole(roleId);
            u.assignRole(roleId); // duplicate

            assertThat(u.getRoles()).hasSize(1);
            assertThat(u.hasRole(roleId)).isTrue();
            assertThat(countEvents(u, RoleAssignedEvent.class)).isEqualTo(1);
        }

        @Test
        @DisplayName("unassignRole() removes a previously assigned role")
        void unassignRole() {
            User u = newUser();
            RoleId roleId = RoleId.generate();
            u.assignRole(roleId);

            u.unassignRole(roleId);

            assertThat(u.getRoles()).isEmpty();
            assertThat(u.hasRole(roleId)).isFalse();
        }
    }

    @Nested
    @DisplayName("linkGoogleAccount()")
    class GoogleLinking {

        @Test
        @DisplayName("links a Google sub and is idempotent on re-link of the same sub")
        void link_idempotentSameSub() {
            User u = newUser();
            GoogleSub sub = GoogleSub.of("google-sub-123");

            u.linkGoogleAccount(sub);
            u.linkGoogleAccount(sub); // idempotent

            assertThat(u.isLinkedWithGoogle()).isTrue();
            assertThat(countEvents(u, OAuthProviderLinkedEvent.class)).isEqualTo(1);
        }

        @Test
        @DisplayName("rejects linking a different Google sub")
        void link_differentSubRejected() {
            User u = newUser();
            u.linkGoogleAccount(GoogleSub.of("google-sub-123"));

            assertThatThrownBy(() -> u.linkGoogleAccount(GoogleSub.of("google-sub-999")))
                    .isInstanceOf(OAuthAccountAlreadyLinkedException.class);
        }
    }
}
