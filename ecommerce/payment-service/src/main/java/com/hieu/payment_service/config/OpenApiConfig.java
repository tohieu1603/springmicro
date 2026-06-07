package com.hieu.payment_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(title = "Payment Service API", version = "1.0",
                 description = "Payment microservice — Sepay / MoMo / COD"),
    servers = {
        @Server(url = "http://localhost:8086", description = "Local dev"),
        @Server(url = "http://localhost:8080", description = "API Gateway")
    })
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Paste the raw JWT (no 'Bearer ' prefix).")
public class OpenApiConfig {}
