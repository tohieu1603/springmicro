package com.hieu.auth_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Auth-service entry point.
 *
 * <p>{@code @EnableScheduling} activates the hourly cleanup job in
 * {@link com.hieu.auth_service.infrastructure.security.TokenBlacklistService} that purges
 * already-expired blacklist rows.
 */
@EnableScheduling
@SpringBootApplication
public class AuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}

}
