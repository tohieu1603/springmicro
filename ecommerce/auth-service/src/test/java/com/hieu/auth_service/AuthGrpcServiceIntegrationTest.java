package com.hieu.auth_service;

import com.hieu.auth_service.application.command.RegisterUserCommand;
import com.hieu.auth_service.application.common.CommandHandler;
import com.hieu.auth_service.application.dto.AuthResponseDTO;
import com.hieu.auth_service.application.port.TokenBlacklistPort;
import com.hieu.auth_service.domain.services.TokenProviderPort;
import com.hieu.auth_service.interfaces.grpc.AuthGrpcService;
import com.hieu.auth_service.interfaces.grpc.proto.CheckRoleRequest;
import com.hieu.auth_service.interfaces.grpc.proto.GetUserRequest;
import com.hieu.auth_service.interfaces.grpc.proto.VerifyTokenRequest;
import com.hieu.auth_service.interfaces.grpc.proto.VerifyTokenResponse;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives {@link AuthGrpcService} directly (no network) to verify the plumbing between
 * generated stubs, the application layer, and the blacklist port.
 *
 * <p>Using the impl class instead of a real channel keeps the test fast while still
 * exercising the same code paths downstream consumers hit.
 */
class AuthGrpcServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired AuthGrpcService grpcService;
    @Autowired CommandHandler<RegisterUserCommand, AuthResponseDTO> registerHandler;
    @Autowired TokenProviderPort tokenProvider;
    @Autowired TokenBlacklistPort tokenBlacklist;

    @Test
    void verifyToken_returnsValidTrueForFreshJwt() {
        AuthResponseDTO registered = registerHandler.handle(new RegisterUserCommand(
                "grpcuser", "grpc@example.com", "P@ssw0rd123", "Grp", "User"));

        VerifyTokenResponse response = callVerify(registered.accessToken());

        assertThat(response.getValid()).isTrue();
        assertThat(response.getUsername()).isEqualTo("grpcuser");
        assertThat(response.getUserId()).isEqualTo(registered.user().id());
        assertThat(response.getRolesList()).contains("ROLE_USER");
    }

    @Test
    void verifyToken_returnsFalseWhenBlacklisted() {
        AuthResponseDTO registered = registerHandler.handle(new RegisterUserCommand(
                "blacklisted", "bl@example.com", "P@ssw0rd123", "Bl", "User"));
        var claims = tokenProvider.parseAccessToken(registered.accessToken());

        tokenBlacklist.revoke(claims.tokenId(), claims.userId(),
                Instant.now().plusSeconds(900), "LOGOUT");

        VerifyTokenResponse response = callVerify(registered.accessToken());
        assertThat(response.getValid()).isFalse();
    }

    @Test
    void verifyToken_returnsFalseForGarbageInput() {
        assertThat(callVerify("not-a-jwt").getValid()).isFalse();
    }

    @Test
    void checkRole_returnsTrueForAssignedRole() {
        AuthResponseDTO registered = registerHandler.handle(new RegisterUserCommand(
                "roleuser", "role@example.com", "P@ssw0rd123", "R", "U"));

        AtomicReference<Boolean> hasRole = new AtomicReference<>();
        grpcService.checkRole(CheckRoleRequest.newBuilder()
                        .setUserId(registered.user().id())
                        .setRoleName("ROLE_USER")
                        .build(),
                capture(r -> hasRole.set(r.getHasRole())));

        assertThat(hasRole.get()).isTrue();
    }

    @Test
    void getUser_foundFalseForUnknownId() {
        var captured = new AtomicReference<com.hieu.auth_service.interfaces.grpc.proto.GetUserResponse>();
        grpcService.getUser(GetUserRequest.newBuilder().setUserId("does-not-exist").build(),
                capture(captured::set));

        assertThat(captured.get().getFound()).isFalse();
    }

    // ── Test plumbing ────────────────────────────────────────────────────

    private VerifyTokenResponse callVerify(String accessToken) {
        AtomicReference<VerifyTokenResponse> holder = new AtomicReference<>();
        grpcService.verifyToken(
                VerifyTokenRequest.newBuilder().setAccessToken(accessToken).build(),
                capture(holder::set));
        return holder.get();
    }

    /** Minimal {@link StreamObserver} for unary RPCs that just captures the response. */
    private static <T> StreamObserver<T> capture(java.util.function.Consumer<T> sink) {
        return new StreamObserver<T>() {
            @Override public void onNext(T value)        { sink.accept(value); }
            @Override public void onError(Throwable t)   { throw new AssertionError(t); }
            @Override public void onCompleted()          { /* no-op */ }
        };
    }
}
