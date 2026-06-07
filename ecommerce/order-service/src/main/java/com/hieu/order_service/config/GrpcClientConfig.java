package com.hieu.order_service.config;

import com.hieu.catalog_service.interfaces.grpc.proto.CatalogServiceGrpc;
import com.hieu.cart_service.interfaces.grpc.proto.CartServiceGrpc;
import com.hieu.inventory_service.interfaces.grpc.proto.InventoryServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires gRPC blocking stubs for catalog, cart, and inventory services. */
@Configuration
public class GrpcClientConfig {

    @Bean
    public CatalogServiceGrpc.CatalogServiceBlockingStub catalogServiceBlockingStub(
            @Value("${grpc.client.catalog.address:localhost:9093}") String address) {
        var channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
        return CatalogServiceGrpc.newBlockingStub(channel);
    }

    @Bean
    public CartServiceGrpc.CartServiceBlockingStub cartServiceBlockingStub(
            @Value("${grpc.client.cart.address:localhost:9094}") String address) {
        var channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
        return CartServiceGrpc.newBlockingStub(channel);
    }

    @Bean
    public InventoryServiceGrpc.InventoryServiceBlockingStub inventoryServiceBlockingStub(
            @Value("${grpc.client.inventory.address:localhost:9098}") String address) {
        var channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
        return InventoryServiceGrpc.newBlockingStub(channel);
    }
}
