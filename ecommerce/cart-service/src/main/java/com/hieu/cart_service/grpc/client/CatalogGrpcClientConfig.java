package com.hieu.cart_service.grpc.client;

import com.hieu.catalog_service.interfaces.grpc.proto.CatalogServiceGrpc;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds a plaintext gRPC channel directly via {@link Grpc#newChannelBuilder} so we
 * don't depend on spring-grpc auto-config picking up the negotiation-type — locally
 * we know the catalog server is plaintext.
 */
@Configuration
public class CatalogGrpcClientConfig {

    @Bean
    public ManagedChannel catalogServiceChannel(
            @Value("${catalog.grpc.host:127.0.0.1}") String host,
            @Value("${catalog.grpc.port:9093}") int port) {
        return Grpc.newChannelBuilderForAddress(host, port, InsecureChannelCredentials.create())
                .build();
    }

    @Bean
    public CatalogServiceGrpc.CatalogServiceBlockingStub catalogServiceBlockingStub(
            ManagedChannel catalogServiceChannel) {
        return CatalogServiceGrpc.newBlockingStub(catalogServiceChannel);
    }
}
