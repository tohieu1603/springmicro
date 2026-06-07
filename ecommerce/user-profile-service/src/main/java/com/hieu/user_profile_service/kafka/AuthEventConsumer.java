package com.hieu.user_profile_service.kafka;

import com.hieu.user_profile_service.kafka.event.ProfileUpsertedSpringEvent;
import com.hieu.user_profile_service.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Consumes {@code auth.user-registered} events published by auth-service on new user registration.
 * Seeds a user_profiles row via INSERT ... ON CONFLICT DO NOTHING.
 *
 * NOTE: auth-service does not currently publish this topic. This consumer is ready and will
 * activate automatically once auth-service starts emitting auth.user-registered events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEventConsumer {

    private final UserProfileRepository profileRepo;
    private final ApplicationEventPublisher applicationEventPublisher;

    @KafkaListener(topics = "auth.user-registered", groupId = "user-profile-service")
    @Transactional
    public void onUserRegistered(Map<String, Object> payload) {
        try {
            String userId    = str(payload, "userId");
            String email     = str(payload, "email");
            String firstName = str(payload, "firstName");
            String lastName  = str(payload, "lastName");

            if (userId.isBlank() || email.isBlank()) {
                log.warn("auth.user-registered: missing userId or email — skipping. payload={}", payload);
                return;
            }

            profileRepo.insertIfAbsent(userId, email, firstName, lastName);
            // Publish Spring event — Kafka push deferred to AFTER_COMMIT via ProfileUpsertedListener
            applicationEventPublisher.publishEvent(new ProfileUpsertedSpringEvent(userId, email, firstName, lastName, null));
            log.info("Seeded user_profile for userId={}", userId);
        } catch (Exception e) {
            log.error("auth.user-registered processing failed: {}", e.getMessage(), e);
        }
    }

    private static String str(Map<String, Object> m, String key) {
        var v = m.get(key);
        return v != null ? v.toString() : "";
    }
}
