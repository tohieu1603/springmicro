package com.hieu.auth_service.domain.models.user;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.hieu.auth_service.domain.models.role.vo.RoleId;
import com.hieu.auth_service.domain.models.user.events.AccountStatusChangedEvent;
import com.hieu.auth_service.domain.models.user.events.EmailChangedEvent;
import com.hieu.auth_service.domain.models.user.events.OAuthProviderLinkedEvent;
import com.hieu.auth_service.domain.models.user.events.PasswordChangedEvent;
import com.hieu.auth_service.domain.models.user.events.RoleAssignedEvent;
import com.hieu.auth_service.domain.models.user.events.RoleRemovedEvent;
import com.hieu.auth_service.domain.models.user.events.UserCreatedEvent;
import com.hieu.auth_service.domain.models.user.events.UserLoggedInEvent;
import com.hieu.auth_service.domain.models.user.exceptions.AccountNotUsableException;
import com.hieu.auth_service.domain.models.user.exceptions.InvalidCredentialsException;
import com.hieu.auth_service.domain.models.user.exceptions.OAuthAccountAlreadyLinkedException;
import com.hieu.auth_service.domain.models.user.vo.AccountStatus;
import com.hieu.auth_service.domain.models.user.vo.Email;
import com.hieu.auth_service.domain.models.user.vo.GoogleSub;
import com.hieu.auth_service.domain.models.user.vo.Password;
import com.hieu.auth_service.domain.models.user.vo.PersonName;
import com.hieu.auth_service.domain.models.user.vo.UserId;
import com.hieu.auth_service.domain.models.user.vo.Username;
import com.hieu.auth_service.domain.services.PasswordEncoderPort;
import com.hieu.auth_service.domain.shared.AggregateRoot;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * User aggregate root.
 *
 * <p>Encapsulates identity, credentials, account status, role assignments, and token
 * versioning. State transitions go through explicit business methods that enforce
 * invariants and raise {@link com.hieu.auth_service.domain.events.DomainEvent}s.
 * Event management is inherited from {@link AggregateRoot}; events are drained by
 * infrastructure via {@code pullDomainEvents()} after a successful save.
 */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(of = {"id", "username", "email", "accountStatus"})
public final class User extends AggregateRoot {

    @EqualsAndHashCode.Include
    private UserId id;
    private Username username;
    private Email email;
    private Password password;
    private PersonName personName;
    private AccountStatus accountStatus;
    private Set<RoleId> roles;
    private int tokenVersion;
    private GoogleSub googleSub;         
    private Instant createdAt;
    private Instant updatedAt;

    private User() {
        this.roles = new HashSet<>();
    }

    // ── Factories ──────────────────────────────────────────────────────────

    /**
     * Registers a brand-new user. The raw password is hashed here via the encoder port
     * — plaintext never reaches persistence.
     */
    public static User register(Username username, Email email, Password rawPassword,
                                PersonName personName, PasswordEncoderPort encoder) {
        Objects.requireNonNull(encoder, "encoder");
        if (!rawPassword.needsEncoding()) {
            throw new IllegalArgumentException("register() requires a raw password");
        }

        User u = new User();
        u.id = UserId.generate();
        u.username = username;
        u.email = email;
        u.password = Password.createEncoded(encoder.encode(rawPassword.value()));
        u.personName = personName;
        u.accountStatus = AccountStatus.createActive();
        u.tokenVersion = 1;
        u.createdAt = Instant.now();
        u.updatedAt = u.createdAt;

        u.registerEvent(new UserCreatedEvent(u.id.value(), username.value(), email.value()));
        return u;
    }

    /** Rebuilds an aggregate from persistent state — used only by repositories. */
    public static User reconstitute(UserId id, Username username, Email email, Password password,
                                    PersonName personName, AccountStatus status, Set<RoleId> roles,
                                    int tokenVersion, GoogleSub googleSub,
                                    Instant createdAt, Instant updatedAt) {
        User u = new User();
        u.id = id;
        u.username = username;
        u.email = email;
        u.password = password;
        u.personName = personName;
        u.accountStatus = status;
        u.roles = new HashSet<>(roles);
        u.tokenVersion = tokenVersion;
        u.googleSub = googleSub;
        u.createdAt = createdAt;
        u.updatedAt = updatedAt;
        return u;
    }

    /**
     * Creates a brand-new account from a verified Google identity. The user
     * gets a random unguessable BCrypt-encoded password so the password-login
     * path always rejects them — they must keep using Google until they
     * explicitly set a password via {@link #setInitialPassword}.
     *
     * <p>Caller responsibility: ensure {@code username} is unique (typically
     * derived from email's local-part with a numeric suffix if needed).
     */
    public static User registerFromGoogle(Username username, Email email, PersonName personName,
                                          GoogleSub googleSub, PasswordEncoderPort encoder) {
        Objects.requireNonNull(googleSub, "googleSub");
        Objects.requireNonNull(encoder, "encoder");

        User u = new User();
        u.id = UserId.generate();
        u.username = username;
        u.email = email;
        // Random hash → guaranteed not to match anything a human would type.
        u.password = Password.createEncoded(encoder.encode(UUID.randomUUID().toString()));
        u.personName = personName;
        u.accountStatus = AccountStatus.createActive();
        u.tokenVersion = 1;
        u.googleSub = googleSub;
        u.createdAt = Instant.now();
        u.updatedAt = u.createdAt;

        u.registerEvent(new UserCreatedEvent(u.id.value(), username.value(), email.value()));
        u.registerEvent(new OAuthProviderLinkedEvent(
                u.id.value(), "google", googleSub.value(), /*newAccount*/ true));
        return u;
    }

    /**
     * Links an existing password account with a Google identity. Idempotent
     * when the same {@code sub} is re-linked. Throws when a <i>different</i>
     * Google sub is already attached — we don't support multiple Google
     * identities per HIEU account today.
     */
    public void linkGoogleAccount(GoogleSub sub) {
        Objects.requireNonNull(sub, "sub");
        if (googleSub != null) {
            if (googleSub.equals(sub)) return;          // idempotent re-link
            throw new OAuthAccountAlreadyLinkedException("google");
        }
        googleSub = sub;
        updatedAt = Instant.now();
        registerEvent(new OAuthProviderLinkedEvent(
                id.value(), "google", sub.value(), /*newAccount*/ false));
    }

    /** Whether this account has a Google identity bound to it. */
    public boolean isLinkedWithGoogle() { return googleSub != null; }

    // ── Authentication ────────────────────────────────────────────────────

    /**
     * Verifies credentials and records the login. Throws {@link AccountNotUsableException}
     * if the account is currently disabled/locked/expired — callers differentiate between
     * "wrong password" and "unusable account" via distinct exception codes.
     */
    public boolean authenticate(Password rawPassword, PasswordEncoderPort encoder) {
        ensureAuthenticatable();

        boolean matches = encoder.matches(rawPassword.value(), password.value());
        if (matches) {
            recordLogin();
        }
        return matches;
    }

    /**
     * Throws {@link AccountNotUsableException} if any of the four account-status flags
     * forbids authentication. Public so domain services can run the same check
     * standalone (e.g. before issuing a token).
     */
    public void ensureAuthenticatable() {
        var s = accountStatus;
        if (!s.enabled())               throw new AccountNotUsableException(AccountNotUsableException.Reason.DISABLED);
        if (!s.accountNonLocked())      throw new AccountNotUsableException(AccountNotUsableException.Reason.LOCKED);
        if (!s.accountNonExpired())     throw new AccountNotUsableException(AccountNotUsableException.Reason.EXPIRED);
        if (!s.credentialsNonExpired()) throw new AccountNotUsableException(AccountNotUsableException.Reason.CREDENTIALS_EXPIRED);
    }

    private void recordLogin() {
        Instant now = Instant.now();
        accountStatus = accountStatus.withLastLogin(now);
        updatedAt = now;
        registerEvent(new UserLoggedInEvent(id.value(), username.value()));
    }

    // ── Credentials ───────────────────────────────────────────────────────

    /**
     * Changes the password after verifying the old one and invalidates all outstanding
     * JWTs by bumping {@link #tokenVersion}.
     */
    public void changePassword(Password oldPassword, Password newPassword, PasswordEncoderPort encoder) {
        if (!encoder.matches(oldPassword.value(), password.value())) {
            throw new InvalidCredentialsException();
        }

        password = newPassword.needsEncoding()
                ? Password.createEncoded(encoder.encode(newPassword.value()))
                : newPassword;

        incrementTokenVersion();
        registerEvent(new PasswordChangedEvent(id.value(), username.value()));
    }

    public void updateEmail(Email newEmail) {
        if (email.equals(newEmail)) return;
        Email oldEmail = email;
        email = newEmail;
        updatedAt = Instant.now();
        registerEvent(new EmailChangedEvent(id.value(), oldEmail.value(), newEmail.value()));
    }

    public void updatePersonName(PersonName newName) {
        if (personName.equals(newName)) return;
        personName = newName;
        updatedAt = Instant.now();
    }

    // ── Account-status transitions ────────────────────────────────────────

    public void lock()    { transitionStatus(accountStatus.lock(),    AccountStatusChangedEvent.Transition.LOCKED); }
    public void unlock()  { transitionStatus(accountStatus.unlock(),  AccountStatusChangedEvent.Transition.UNLOCKED); }
    public void disable() { transitionStatus(accountStatus.disable(), AccountStatusChangedEvent.Transition.DISABLED); }
    public void enable()  { transitionStatus(accountStatus.enable(),  AccountStatusChangedEvent.Transition.ENABLED); }

    private void transitionStatus(AccountStatus next, AccountStatusChangedEvent.Transition kind) {
        if (accountStatus.equals(next)) return;
        accountStatus = next;
        updatedAt = Instant.now();
        registerEvent(new AccountStatusChangedEvent(id.value(), username.value(), kind));
    }

    public boolean isActive() {
        return accountStatus.isActive();
    }

    // ── Role management ──────────────────────────────────────────────────

    public void assignRole(RoleId roleId) {
        if (roles.add(roleId)) {
            updatedAt = Instant.now();
            registerEvent(new RoleAssignedEvent(id.value(), roleId.value()));
        }
    }

    public void unassignRole(RoleId roleId) {
        if (roles.remove(roleId)) {
            updatedAt = Instant.now();
            registerEvent(new RoleRemovedEvent(id.value(), roleId.value()));
        }
    }

    public boolean hasRole(RoleId roleId) { return roles.contains(roleId); }

    /** Unmodifiable view of assigned role ids. */
    public Set<RoleId> getRoles() { return Collections.unmodifiableSet(roles); }

    // ── Token version ────────────────────────────────────────────────────

    /**
     * Bumps the token version to invalidate every outstanding JWT for this user.
     * Called on password change, admin-forced revocation, suspected compromise, etc.
     */
    public void incrementTokenVersion() {
        tokenVersion++;
        updatedAt = Instant.now();
    }
}
