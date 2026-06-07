package com.hieu.api_gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Top-level OpenAPI descriptor for the gateway.
 *
 * <p>Individual service specs are aggregated via {@code springdoc.swagger-ui.urls} in
 * {@code application.yaml} — Swagger UI shows a dropdown so testers can pick a service
 * and hit endpoints through the gateway URL.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("hieu.com API Gateway")
                        .version("1.0")
                        .description("Unified entry point for all ecommerce microservices.")
                        .contact(new Contact().name("API Support").email("support@hieu.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .addServersItem(new Server().url("http://localhost:8080").description("Local gateway"));
    }
}
