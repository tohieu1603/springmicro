package com.hieu.flash_sale_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI / Swagger UI configuration. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Flash Sale Service API")
                        .description("Atomic slot reservation with Redis + Kafka event publishing")
                        .version("0.0.1-SNAPSHOT"));
    }
}
