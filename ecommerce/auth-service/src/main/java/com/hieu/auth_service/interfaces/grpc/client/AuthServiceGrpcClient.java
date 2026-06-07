package com.hieu.auth_service.interfaces.grpc.client;

import com.hieu.auth_service.interfaces.grpc.proto.AuthServiceGrpc;
import com.hieu.auth_service.interfaces.grpc.proto.CheckPermissionRequest;
import com.hieu.auth_service.interfaces.grpc.proto.CheckPermissionResponse;
import com.hieu.auth_service.interfaces.grpc.proto.CheckRoleRequest;
import com.hieu.auth_service.interfaces.grpc.proto.CheckRoleResponse;
import com.hieu.auth_service.interfaces.grpc.proto.GetUserRequest;
import com.hieu.auth_service.interfaces.grpc.proto.GetUserResponse;
import com.hieu.auth_service.interfaces.grpc.proto.VerifyTokenRequest;
import com.hieu.auth_service.interfaces.grpc.proto.VerifyTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Ergonomic wrapper around the generated {@link AuthServiceGrpc.AuthServiceBlockingStub}.
 *
 * <p>Hides the {@code *Request.newBuilder().setX(...).build()} boilerplate and applies a
 * sensible default per-call deadline (200 ms). Domain services depend on
 * {@code AuthServiceGrpcClient} instead of the generated stub so the generated types
 * only leak through this one adapter.
 *
 * <p>Keeps the wrapper {@code final} — extensions should wrap, not subclass.
 */
@Component
@ConditionalOnProperty(prefix = "grpc.client.auth-service", name = "address")
@RequiredArgsConstructor
public final class AuthServiceGrpcClient {

    /** Default per-call deadline; tight by design — auth calls are on the hot path. */
    private static final long DEFAULT_DEADLINE_MS = 200L;

    private final AuthServiceGrpc.AuthServiceBlockingStub stub;

    /**
     * Parses + signature-verifies an access JWT remotely.
     *
     * @param accessToken opaque bearer token
     * @return full {@link VerifyTokenResponse} — caller branches on {@code valid}
     */
    public VerifyTokenResponse verifyToken(String accessToken) {
        return withDeadline().verifyToken(
                VerifyTokenRequest.newBuilder().setAccessToken(accessToken).build());
    }

    /**
     * Checks role membership.
     *
     * @param userId   target user id
     * @param roleName canonical role name (e.g. {@code ROLE_ADMIN})
     * @return {@code true} when the user has the role
     */
    public boolean hasRole(String userId, String roleName) {
        CheckRoleResponse response = withDeadline().checkRole(
                CheckRoleRequest.newBuilder()
                        .setUserId(userId)
                        .setRoleName(roleName)
                        .build());
        return response.getHasRole();
    }

    /**
     * Checks fine-grained permission.
     *
     * @param userId          target user id
     * @param permissionName  canonical permission name (e.g. {@code USER_READ})
     * @return {@code true} when any of the user's roles grants the permission
     */
    public boolean hasPermission(String userId, String permissionName) {
        CheckPermissionResponse response = withDeadline().checkPermission(
                CheckPermissionRequest.newBuilder()
                        .setUserId(userId)
                        .setPermissionName(permissionName)
                        .build());
        return response.getAllowed();
    }

    /**
     * Fetches a remote user profile.
     *
     * @param userId target user id
     * @return response carrying {@code found=false} when the user does not exist
     */
    public GetUserResponse getUser(String userId) {
        return withDeadline().getUser(
                GetUserRequest.newBuilder().setUserId(userId).build());
    }

    /** Applies the default deadline to every call. Override per-call if needed. */
    private AuthServiceGrpc.AuthServiceBlockingStub withDeadline() {
        return stub.withDeadlineAfter(DEFAULT_DEADLINE_MS, TimeUnit.MILLISECONDS);
    }
}
