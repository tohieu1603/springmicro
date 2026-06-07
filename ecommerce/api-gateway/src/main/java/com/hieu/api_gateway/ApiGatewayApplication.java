package com.hieu.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Edge gateway — the single entry point every external request hits.
 *
 * <p>Runs on the reactive stack (Spring WebFlux + Spring Cloud Gateway). Eureka
 * discovery is enabled so routes can target {@code lb://service-name} and follow
 * instances as they come and go. Actual route table is declared in
 * {@code application.yaml}; the Java config layer holds cross-cutting filters
 * (logging, JWT auth, blacklist check) that need code — not YAML — to express.
 */
@EnableDiscoveryClient
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
