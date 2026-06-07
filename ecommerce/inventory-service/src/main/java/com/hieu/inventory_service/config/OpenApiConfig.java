package com.hieu.inventory_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI / Swagger configuration. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI inventoryOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Inventory Service API")
                .version("1.0")
                .description("Stock management, reservation, gRPC bridge"));
    }
}
