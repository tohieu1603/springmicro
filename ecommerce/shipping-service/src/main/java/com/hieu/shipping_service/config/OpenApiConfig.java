package com.hieu.shipping_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger / OpenAPI 3 configuration. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI shippingOpenAPI() {
        var scheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
        return new OpenAPI()
                .info(new Info().title("Shipping Service API").version("1.0"))
                .components(new Components().addSecuritySchemes("bearerAuth", scheme))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
