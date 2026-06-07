package com.hieu.catalog_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Catalog service entry point.
 *
 * <p>DDD / Hexagonal / CQRS scaffold — aligned with auth-service. Exposes
 * HTTP (8083) + gRPC (9093). Publishes product/variant lifecycle events to
 * Kafka so inventory-service can snapshot stock on creation.
 */
@EnableDiscoveryClient
@SpringBootApplication
public class CatalogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }
}
