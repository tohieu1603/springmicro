package com.hieu.user_profile_service.kafka.listener;

import com.hieu.user_profile_service.kafka.UserProfileEventPublisher;
import com.hieu.user_profile_service.kafka.event.ProfileUpsertedSpringEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Fires Kafka publish after DB transaction commits — guarantees the row is visible
 * before downstream services receive the event (AFTER_COMMIT prevents phantom reads).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProfileUpsertedListener {

    private final UserProfileEventPublisher kafkaPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ProfileUpsertedSpringEvent ev) {
        try {
            kafkaPublisher.publishProfileUpserted(ev.userId(), ev.email(), ev.firstName(), ev.lastName(), ev.phone());
        } catch (Exception e) {
            log.error("Kafka publish failed for {}: {}", ev.userId(), e.getMessage());
        }
    }
}
