package com.hieu.catalog_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Catalog Service API",
        version = "1.0",
        description = "Product catalog microservice (DDD / Hexagonal / CQRS).",
        contact = @Contact(name = "API Support", email = "support@hieu.com"),
        license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0.html")),
    servers = {
        @Server(url = "http://localhost:8083", description = "Local dev"),
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
