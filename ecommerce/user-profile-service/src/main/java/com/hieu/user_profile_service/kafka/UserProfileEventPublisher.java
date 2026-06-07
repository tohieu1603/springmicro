package com.hieu.user_profile_service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Publishes profile change events so downstream services (notification, etc.) can
 * cache the email locally instead of doing per-event gRPC lookups.
 *
 * <p>Topic: {@code user.profile-upserted}. Key = userId so consumers in the same
 * partition see events in order.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserProfileEventPublisher {

    public static final String TOPIC_PROFILE_UPSERTED = "user.profile-upserted";

    private final KafkaTemplate<String, String> stringKafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishProfileUpserted(String userId, String email,
                                       String firstName, String lastName, String phone) {
        if (userId == null || userId.isBlank() || email == null || email.isBlank()) {
            log.warn("Skip user.profile-upserted publish — missing userId or email");
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId);
        payload.put("email", email);
        payload.put("firstName", firstName);
        payload.put("lastName", lastName);
        payload.put("phone", phone);
        try {
            String json = objectMapper.writeValueAsString(payload);
            stringKafkaTemplate.send(TOPIC_PROFILE_UPSERTED, userId, json);
            log.debug("Published {} for userId={}", TOPIC_PROFILE_UPSERTED, userId);
        } catch (Exception e) {
            // Jackson 3 throws JacksonException (RuntimeException) — keep narrow catch via Exception.
            log.error("Failed to publish user.profile-upserted: {}", e.getMessage(), e);
        }
    }
}
