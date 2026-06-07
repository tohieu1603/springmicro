package com.hieu.notification_service.kafka;

import com.hieu.notification_service.service.UserProfileEmailResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Listens for {@code user.profile-upserted} events emitted by user-profile-service
 * and warms the local Redis cache so subsequent order/payment/shipping notifications
 * can resolve emails without a gRPC round-trip.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserProfileEventConsumer {

    private final UserProfileEmailResolver emailResolver;

    @KafkaListener(topics = "user.profile-upserted", groupId = "notification-service")
    public void onProfileUpserted(Map<String, Object> payload) {
        try {
            String userId = str(payload, "userId");
            String email  = str(payload, "email");
            if (userId.isBlank() || email.isBlank()) {
                log.debug("user.profile-upserted: missing userId/email, skipping. payload={}", payload);
                return;
            }
            emailResolver.cache(userId, email);
            log.debug("Cached email for userId={}", userId);
        } catch (Exception e) {
            log.error("user.profile-upserted processing failed: {}", e.getMessage(), e);
        }
    }

    private static String str(Map<String, Object> m, String key) {
        var v = m.get(key);
        return v != null ? v.toString() : "";
    }
}
