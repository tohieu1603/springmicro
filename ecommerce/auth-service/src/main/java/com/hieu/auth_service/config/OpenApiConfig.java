package com.hieu.auth_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Springdoc / OpenAPI configuration.
 *
 * <p>The {@code @SecurityScheme(name = "bearerAuth", ...)} declaration must match the
 * {@code @SecurityRequirement(name = "bearerAuth")} used on individual controllers —
 * otherwise Swagger UI's "Authorize" button silently does nothing. Previous config
 * had a mismatched {@code "Bearer Authentication"} name which broke the flow.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Auth Service API",
                version = "1.0",
                description = "Authentication + authorization microservice (DDD / Hexagonal / CQRS).",
                contact = @Contact(name = "API Support", email = "support@hieu.com"),
                license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0.html")),
        servers = {
                @Server(url = "http://localhost:8081", description = "Local dev"),
                @Server(url = "http://localhost:8080", description = "API Gateway")
        })
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Paste the raw JWT (no 'Bearer ' prefix)."
)
public class OpenApiConfig {
}
