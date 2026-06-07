package com.hieu.notification_service.config;

import org.springframework.context.annotation.Configuration;

/**
 * Mail configuration is driven entirely by {@code spring.mail.*} in application.yaml.
 * Spring Boot autoconfigures {@link org.springframework.mail.javamail.JavaMailSender} automatically.
 * This class is a placeholder for future customisation (e.g. TLS profiles, retries).
 */
@Configuration
public class MailConfig {
    // JavaMailSender auto-configured by spring-boot-starter-mail + application.yaml properties
}
