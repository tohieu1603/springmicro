package com.hieu.user_profile_service.kafka.listener;

import com.hieu.user_profile_service.kafka.UserProfileEventPublisher;
import com.hieu.user_profile_service.kafka.event.ProfileUpsertedSpringEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Pure unit tests for {@link ProfileUpsertedListener#on(ProfileUpsertedSpringEvent)} —
 * the AFTER_COMMIT bridge that forwards a Spring event to the Kafka publisher.
 *
 * <p>Asserts the listener unpacks the record's fields into the publisher call, and that a
 * publisher exception is caught (logged, not re-thrown) so a Kafka outage cannot disrupt
 * the already-committed transaction.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileUpsertedListener (unit)")
class ProfileUpsertedListenerTest {

    @Mock
    private UserProfileEventPublisher kafkaPublisher;

    @InjectMocks
    private ProfileUpsertedListener listener;

    @Test
    @DisplayName("forwards all event fields to the Kafka publisher")
    void on_forwardsFieldsToPublisher() {
        ProfileUpsertedSpringEvent ev =
                new ProfileUpsertedSpringEvent("u-1", "a@test.com", "Ann", "Lee", "555-1234");

        listener.on(ev);

        verify(kafkaPublisher).publishProfileUpserted("u-1", "a@test.com", "Ann", "Lee", "555-1234");
    }

    @Test
    @DisplayName("null phone is forwarded unchanged")
    void on_nullPhone_forwarded() {
        ProfileUpsertedSpringEvent ev =
                new ProfileUpsertedSpringEvent("u-2", "b@test.com", "Bob", "Kim", null);

        listener.on(ev);

        verify(kafkaPublisher).publishProfileUpserted("u-2", "b@test.com", "Bob", "Kim", null);
    }

    @Test
    @DisplayName("publisher exception is swallowed — does not propagate to caller")
    void on_publisherThrows_isSwallowed() {
        doThrow(new RuntimeException("Kafka broker down"))
                .when(kafkaPublisher)
                .publishProfileUpserted(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());

        ProfileUpsertedSpringEvent ev =
                new ProfileUpsertedSpringEvent("u-3", "c@test.com", "Cy", "Lo", null);

        assertThatCode(() -> listener.on(ev)).doesNotThrowAnyException();
    }
}
