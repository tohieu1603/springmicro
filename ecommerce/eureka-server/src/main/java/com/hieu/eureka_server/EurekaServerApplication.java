package com.hieu.eureka_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Netflix Eureka service registry — central directory every microservice registers with
 * on startup and queries when it needs to call a sibling service.
 *
 * <p>Protected by HTTP basic auth (see {@code EurekaSecurityConfig}); Eureka clients pass
 * credentials via their {@code eureka.client.service-url.defaultZone} URL.
 */
@EnableEurekaServer
@SpringBootApplication
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
