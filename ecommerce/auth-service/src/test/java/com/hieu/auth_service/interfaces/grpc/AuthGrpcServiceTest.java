package com.hieu.auth_service.interfaces.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hieu.auth_service.application.common.QueryHandler;
import com.hieu.auth_service.application.dto.UserDTO;
import com.hieu.auth_service.application.port.TokenBlacklistPort;
import com.hieu.auth_service.application.query.CheckPermissionQuery;
import com.hieu.auth_service.application.query.CheckRoleQuery;
import com.hieu.auth_service.application.query.GetUserByIdQuery;
import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.exceptions.UserNotFoundException;
import com.hieu.auth_service.domain.models.user.vo.AccountStatus;
import com.hieu.auth_service.domain.models.user.vo.Email;
import com.hieu.auth_service.domain.models.user.vo.Password;
import com.hieu.auth_service.domain.models.user.vo.PersonName;
import com.hieu.auth_service.domain.models.user.vo.UserId;
import com.hieu.auth_service.domain.models.user.vo.Username;
import com.hieu.auth_service.domain.repositories.UserRepository;
import com.hieu.auth_service.domain.services.TokenProviderPort;
import com.hieu.auth_service.domain.services.TokenProviderPort.AccessClaims;
import com.hieu.auth_service.interfaces.grpc.proto.CheckPermissionRequest;
import com.hieu.auth_service.interfaces.grpc.proto.CheckPermissionResponse;
import com.hieu.auth_service.interfaces.grpc.proto.CheckRoleRequest;
import com.hieu.auth_service.interfaces.grpc.proto.CheckRoleResponse;
import com.hieu.auth_service.interfaces.grpc.proto.GetUserRequest;
import com.hieu.auth_service.interfaces.grpc.proto.GetUserResponse;
import com.hieu.auth_service.interfaces.grpc.proto.VerifyTokenRequest;
import com.hieu.auth_service.interfaces.grpc.proto.VerifyTokenResponse;

import io.grpc.stub.StreamObserver;

/**
 * Pure unit tests for {@link AuthGrpcService}: domain/DTO <-> protobuf response mapping and the
 * branching that the integration test does not exercise (blacklist short-circuit, null-safe
 * string normalisation, the {@code active} AND-of-four-flags rule, null roles/permissions, and
 * the UserNotFound / IllegalArgument -> found=false contract). Collaborators are mocked.
 */
@ExtendWith(MockitoExtension.class)
class AuthGrpcServiceTest {

    @Mock TokenProviderPort tokenProvider;
    @Mock TokenBlacklistPort tokenBlacklist;
    @Mock UserRepository userRepository;
    @Mock QueryHandler<CheckRoleQuery, Boolean> checkRoleHandler;
    @Mock QueryHandler<CheckPermissionQuery, Boolean> checkPermissionHandler;
    @Mock QueryHandler<GetUserByIdQuery, UserDTO> getUserByIdHandler;

    /** Stable UUID used as the token subject so {@link UserId} validation passes. */
    private static final String UID = "11111111-1111-1111-1111-111111111111";

    private AuthGrpcService service() {
        return new AuthGrpcService(tokenProvider, tokenBlacklist, userRepository,
                checkRoleHandler, checkPermissionHandler, getUserByIdHandler);
    }

    /** Builds an active domain user with the given id + tokenVersion for the verifyToken cross-check. */
    private static User activeUser(String id, int tokenVersion) {
        return User.reconstitute(
                UserId.of(id), Username.of("alice"), Email.of("alice@example.com"),
                Password.createEncoded("$2a$dummyhash"), PersonName.of("Alice", "Smith"),
                AccountStatus.createActive(), Set.of(), tokenVersion, null,
                Instant.now(), Instant.now());
    }

    /** Minimal unary StreamObserver capturing onNext. */
    private static <T> Capturing<T> capture() {
        return new Capturing<>();
    }

    private static final class Capturing<T> implements StreamObserver<T> {
        T value;
        boolean completed;
        @Override public void onNext(T v) { this.value = v; }
        @Override public void onError(Throwable t) { throw new AssertionError(t); }
        @Override public void onCompleted() { this.completed = true; }
    }

    private static AccessClaims claims(Set<String> roles) {
        return new AccessClaims("jti-1", UID, "alice", 7, roles,
                Instant.now().plusSeconds(900));
    }

    // ── verifyToken ─────────────────────────────────────────────────────────

    @Test
    void verifyToken_validNotBlacklisted_mapsClaimsToResponse() {
        when(tokenProvider.parseAccessToken("good")).thenReturn(claims(Set.of("ROLE_USER", "ROLE_ADMIN")));
        when(tokenBlacklist.isRevoked("jti-1")).thenReturn(false);
        when(userRepository.findById(UserId.of(UID))).thenReturn(Optional.of(activeUser(UID, 7)));

        Capturing<VerifyTokenResponse> obs = capture();
        service().verifyToken(VerifyTokenRequest.newBuilder().setAccessToken("good").build(), obs);

        VerifyTokenResponse r = obs.value;
        assertThat(r.getValid()).isTrue();
        assertThat(r.getUserId()).isEqualTo(UID);
        assertThat(r.getUsername()).isEqualTo("alice");
        assertThat(r.getTokenVersion()).isEqualTo(7);
        assertThat(r.getRolesList()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        assertThat(obs.completed).isTrue();
    }

    @Test
    void verifyToken_blacklisted_returnsInvalidWithoutClaims() {
        when(tokenProvider.parseAccessToken("revoked")).thenReturn(claims(Set.of("ROLE_USER")));
        when(tokenBlacklist.isRevoked("jti-1")).thenReturn(true);

        Capturing<VerifyTokenResponse> obs = capture();
        service().verifyToken(VerifyTokenRequest.newBuilder().setAccessToken("revoked").build(), obs);

        assertThat(obs.value.getValid()).isFalse();
        assertThat(obs.value.getUserId()).isEmpty();
        assertThat(obs.value.getRolesList()).isEmpty();
    }

    @Test
    void verifyToken_parseThrows_returnsInvalid() {
        when(tokenProvider.parseAccessToken("garbage")).thenThrow(new RuntimeException("bad token"));

        Capturing<VerifyTokenResponse> obs = capture();
        service().verifyToken(VerifyTokenRequest.newBuilder().setAccessToken("garbage").build(), obs);

        assertThat(obs.value.getValid()).isFalse();
        assertThat(obs.completed).isTrue();
    }

    @Test
    void verifyToken_tokenVersionSuperseded_returnsInvalid() {
        when(tokenProvider.parseAccessToken("stale")).thenReturn(claims(Set.of("ROLE_USER")));
        when(tokenBlacklist.isRevoked("jti-1")).thenReturn(false);
        // Live user carries a newer tokenVersion (e.g. after a password change) than the token.
        when(userRepository.findById(UserId.of(UID))).thenReturn(Optional.of(activeUser(UID, 8)));

        Capturing<VerifyTokenResponse> obs = capture();
        service().verifyToken(VerifyTokenRequest.newBuilder().setAccessToken("stale").build(), obs);

        assertThat(obs.value.getValid()).isFalse();
    }

    @Test
    void verifyToken_inactiveAccount_returnsInvalid() {
        when(tokenProvider.parseAccessToken("locked")).thenReturn(claims(Set.of("ROLE_USER")));
        when(tokenBlacklist.isRevoked("jti-1")).thenReturn(false);
        User lockedUser = User.reconstitute(
                UserId.of(UID), Username.of("alice"), Email.of("alice@example.com"),
                Password.createEncoded("$2a$dummyhash"), PersonName.of("Alice", "Smith"),
                AccountStatus.createLocked(), Set.of(), 7, null, Instant.now(), Instant.now());
        when(userRepository.findById(UserId.of(UID))).thenReturn(Optional.of(lockedUser));

        Capturing<VerifyTokenResponse> obs = capture();
        service().verifyToken(VerifyTokenRequest.newBuilder().setAccessToken("locked").build(), obs);

        assertThat(obs.value.getValid()).isFalse();
    }

    @Test
    void verifyToken_userNotFound_returnsInvalid() {
        when(tokenProvider.parseAccessToken("ghost")).thenReturn(claims(Set.of("ROLE_USER")));
        when(tokenBlacklist.isRevoked("jti-1")).thenReturn(false);
        when(userRepository.findById(UserId.of(UID))).thenReturn(Optional.empty());

        Capturing<VerifyTokenResponse> obs = capture();
        service().verifyToken(VerifyTokenRequest.newBuilder().setAccessToken("ghost").build(), obs);

        assertThat(obs.value.getValid()).isFalse();
    }

    @Test
    void verifyToken_nullUsername_normalisedToEmpty() {
        AccessClaims nullish = new AccessClaims("jti-1", UID, null, 3, Set.of(), Instant.now().plusSeconds(60));
        when(tokenProvider.parseAccessToken("t")).thenReturn(nullish);
        when(tokenBlacklist.isRevoked("jti-1")).thenReturn(false);
        when(userRepository.findById(UserId.of(UID))).thenReturn(Optional.of(activeUser(UID, 3)));

        Capturing<VerifyTokenResponse> obs = capture();
        service().verifyToken(VerifyTokenRequest.newBuilder().setAccessToken("t").build(), obs);

        assertThat(obs.value.getValid()).isTrue();
        assertThat(obs.value.getUserId()).isEqualTo(UID);
        assertThat(obs.value.getUsername()).isEmpty();
    }

    // ── checkRole / checkPermission ─────────────────────────────────────────

    @Test
    void checkRole_delegatesToHandler() {
        when(checkRoleHandler.handle(any(CheckRoleQuery.class))).thenReturn(true);

        Capturing<CheckRoleResponse> obs = capture();
        service().checkRole(CheckRoleRequest.newBuilder().setUserId("u").setRoleName("ROLE_ADMIN").build(), obs);

        assertThat(obs.value.getHasRole()).isTrue();
        assertThat(obs.completed).isTrue();
    }

    @Test
    void checkPermission_delegatesToHandler() {
        when(checkPermissionHandler.handle(any(CheckPermissionQuery.class))).thenReturn(false);

        Capturing<CheckPermissionResponse> obs = capture();
        service().checkPermission(
                CheckPermissionRequest.newBuilder().setUserId("u").setPermissionName("USER_READ").build(), obs);

        assertThat(obs.value.getAllowed()).isFalse();
    }

    // ── getUser ─────────────────────────────────────────────────────────────

    private static UserDTO dto(boolean enabled, boolean nonExpired, boolean nonLocked,
                               boolean credsNonExpired, Set<String> roles, Set<String> perms) {
        return new UserDTO("user-1", "alice", "alice@example.com", "Alice", "Smith",
                enabled, nonExpired, nonLocked, credsNonExpired, roles, perms,
                Instant.now(), Instant.now(), null);
    }

    @Test
    void getUser_found_mapsFieldsAndActiveTrueWhenAllFlagsSet() {
        when(getUserByIdHandler.handle(any(GetUserByIdQuery.class)))
                .thenReturn(dto(true, true, true, true, Set.of("ROLE_USER"), Set.of("USER_READ")));

        Capturing<GetUserResponse> obs = capture();
        service().getUser(GetUserRequest.newBuilder().setUserId("user-1").build(), obs);

        GetUserResponse r = obs.value;
        assertThat(r.getFound()).isTrue();
        assertThat(r.getUserId()).isEqualTo("user-1");
        assertThat(r.getUsername()).isEqualTo("alice");
        assertThat(r.getEmail()).isEqualTo("alice@example.com");
        assertThat(r.getActive()).isTrue();
        assertThat(r.getRolesList()).containsExactly("ROLE_USER");
        assertThat(r.getPermissionsList()).containsExactly("USER_READ");
    }

    @Test
    void getUser_activeFalseWhenAnyFlagClear() {
        when(getUserByIdHandler.handle(any(GetUserByIdQuery.class)))
                .thenReturn(dto(true, true, false, true, Set.of(), Set.of()));   // locked

        Capturing<GetUserResponse> obs = capture();
        service().getUser(GetUserRequest.newBuilder().setUserId("user-1").build(), obs);

        assertThat(obs.value.getFound()).isTrue();
        assertThat(obs.value.getActive()).isFalse();
    }

    @Test
    void getUser_nullRolesAndPermissions_skippedSafely() {
        when(getUserByIdHandler.handle(any(GetUserByIdQuery.class)))
                .thenReturn(dto(true, true, true, true, null, null));

        Capturing<GetUserResponse> obs = capture();
        service().getUser(GetUserRequest.newBuilder().setUserId("user-1").build(), obs);

        assertThat(obs.value.getFound()).isTrue();
        assertThat(obs.value.getRolesList()).isEmpty();
        assertThat(obs.value.getPermissionsList()).isEmpty();
    }

    @Test
    void getUser_userNotFound_returnsFoundFalse() {
        when(getUserByIdHandler.handle(any(GetUserByIdQuery.class)))
                .thenThrow(new UserNotFoundException("user-1"));

        Capturing<GetUserResponse> obs = capture();
        service().getUser(GetUserRequest.newBuilder().setUserId("user-1").build(), obs);

        assertThat(obs.value.getFound()).isFalse();
    }

    @Test
    void getUser_malformedId_returnsFoundFalse() {
        when(getUserByIdHandler.handle(any(GetUserByIdQuery.class)))
                .thenThrow(new IllegalArgumentException("not a uuid"));

        Capturing<GetUserResponse> obs = capture();
        service().getUser(GetUserRequest.newBuilder().setUserId("nope").build(), obs);

        assertThat(obs.value.getFound()).isFalse();
        assertThat(obs.completed).isTrue();
    }
}
