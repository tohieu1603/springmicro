package com.hieu.auth_service.interfaces.grpc.client;

import com.hieu.auth_service.interfaces.grpc.proto.AuthServiceGrpc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

/**
 * Reusable gRPC client configuration — drop-in for any service that needs to call
 * auth-service.
 *
 * <p>Provides two canonical stubs from a single {@link io.grpc.ManagedChannel}:
 * <ul>
 *   <li>{@code authServiceBlockingStub}: synchronous, simplest API for imperative
 *       flows (request/response style)</li>
 *   <li>{@code authServiceFutureStub}: non-blocking, returns a {@code ListenableFuture}
 *       for parallelism / reactive composition</li>
 * </ul>
 *
 * <p>Channel config comes from the {@code grpc.client.auth-service.*} properties (see
 * {@code application.yaml}) — including target URL, negotiation type, and deadlines.
 * {@link GrpcChannelFactory} (Spring gRPC) creates the channel so Boot can manage its
 * lifecycle and tracing interceptors.
 *
 * <p>Cross-service template — move this class to a {@code auth-grpc-client}
 * library module if many services end up consuming it.
 */
@Configuration
@ConditionalOnProperty(prefix = "grpc.client.auth-service", name = "address")
public class GrpcClientConfig {

    /**
     * Logical channel name matched against {@code grpc.client.<name>.*} property keys.
     * Other services should override the property value, not this constant.
     */
    public static final String CHANNEL_NAME = "auth-service";

    /**
     * Creates the gRPC channel for auth-service.
     *
     * @param channelFactory Spring-gRPC factory that picks up {@code grpc.client.*} config
     * @param target         target URL, resolved from {@code grpc.client.auth-service.address}
     * @return live {@link io.grpc.ManagedChannel} managed by the ApplicationContext
     */
    @Bean
    public io.grpc.ManagedChannel authServiceChannel(
            GrpcChannelFactory channelFactory,
            @Value("${grpc.client.auth-service.address:static://localhost:9091}") String target) {
        return channelFactory.createChannel(CHANNEL_NAME);
    }

    /**
     * Blocking stub — use for straight request/response flows.
     *
     * @param channel live gRPC channel
     * @return configured blocking stub
     */
    @Bean
    public AuthServiceGrpc.AuthServiceBlockingStub authServiceBlockingStub(io.grpc.ManagedChannel channel) {
        return AuthServiceGrpc.newBlockingStub(channel);
    }

    /**
     * Future stub — use for non-blocking / parallel calls.
     *
     * @param channel live gRPC channel
     * @return configured future stub
     */
    @Bean
    public AuthServiceGrpc.AuthServiceFutureStub authServiceFutureStub(io.grpc.ManagedChannel channel) {
        return AuthServiceGrpc.newFutureStub(channel);
    }
}
