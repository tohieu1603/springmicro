package com.hieu.user_profile_service.kafka;

import com.hieu.user_profile_service.kafka.event.ProfileUpsertedSpringEvent;
import com.hieu.user_profile_service.repository.UserProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Pure unit tests for {@link AuthEventConsumer#onUserRegistered(Map)}.
 *
 * <p>Verifies the seed-on-registration path: a valid payload triggers
 * {@code profileRepo.insertIfAbsent(...)} followed by publication of a
 * {@link ProfileUpsertedSpringEvent}; payloads missing/blank userId or email are
 * skipped entirely (no insert, no event). {@code ProfileUpsertedSpringEvent} is a
 * plain record (not an {@code ApplicationEvent}), so publication is verified with
 * {@code publishEvent(any(ProfileUpsertedSpringEvent.class))} to bind the correct overload.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthEventConsumer (unit)")
class AuthEventConsumerTest {

    @Mock
    private UserProfileRepository profileRepo;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private AuthEventConsumer consumer;

    private static Map<String, Object> payload(String userId, String email,
                                               String firstName, String lastName) {
        Map<String, Object> m = new HashMap<>();
        if (userId != null) m.put("userId", userId);
        if (email != null) m.put("email", email);
        if (firstName != null) m.put("firstName", firstName);
        if (lastName != null) m.put("lastName", lastName);
        return m;
    }

    @Nested
    @DisplayName("Valid payload")
    class ValidPayload {

        @Test
        @DisplayName("inserts profile and publishes ProfileUpsertedSpringEvent")
        void validPayload_insertsAndPublishes() {
            consumer.onUserRegistered(payload("u-1", "a@test.com", "Ann", "Lee"));

            verify(profileRepo).insertIfAbsent("u-1", "a@test.com", "Ann", "Lee");
            verify(applicationEventPublisher).publishEvent(any(ProfileUpsertedSpringEvent.class));
        }

        @Test
        @DisplayName("published event carries userId/email/names and null phone")
        void validPayload_eventCarriesPayloadFields() {
            consumer.onUserRegistered(payload("u-2", "b@test.com", "Bob", "Kim"));

            ArgumentCaptor<ProfileUpsertedSpringEvent> captor =
                    ArgumentCaptor.forClass(ProfileUpsertedSpringEvent.class);
            verify(applicationEventPublisher).publishEvent(captor.capture());

            ProfileUpsertedSpringEvent ev = captor.getValue();
            assertThat(ev.userId()).isEqualTo("u-2");
            assertThat(ev.email()).isEqualTo("b@test.com");
            assertThat(ev.firstName()).isEqualTo("Bob");
            assertThat(ev.lastName()).isEqualTo("Kim");
            assertThat(ev.phone()).isNull();
        }

        @Test
        @DisplayName("missing firstName/lastName default to empty string, still inserts")
        void validPayload_missingNames_defaultEmptyAndInserts() {
            consumer.onUserRegistered(payload("u-3", "c@test.com", null, null));

            verify(profileRepo).insertIfAbsent("u-3", "c@test.com", "", "");
            verify(applicationEventPublisher).publishEvent(any(ProfileUpsertedSpringEvent.class));
        }

        @Test
        @DisplayName("non-String payload values are coerced via toString()")
        void validPayload_nonStringValues_coerced() {
            Map<String, Object> m = new HashMap<>();
            m.put("userId", 42);
            m.put("email", "num@test.com");
            m.put("firstName", "N");
            m.put("lastName", "M");

            consumer.onUserRegistered(m);

            verify(profileRepo).insertIfAbsent("42", "num@test.com", "N", "M");
            verify(applicationEventPublisher).publishEvent(any(ProfileUpsertedSpringEvent.class));
        }
    }

    @Nested
    @DisplayName("Invalid payload — skipped (no insert, no publish)")
    class InvalidPayload {

        @Test
        @DisplayName("missing userId key is skipped")
        void missingUserId_skips() {
            consumer.onUserRegistered(payload(null, "a@test.com", "Ann", "Lee"));

            verify(profileRepo, never()).insertIfAbsent(any(), any(), any(), any());
            verify(applicationEventPublisher, never()).publishEvent(any(ProfileUpsertedSpringEvent.class));
        }

        @Test
        @DisplayName("blank userId is skipped")
        void blankUserId_skips() {
            consumer.onUserRegistered(payload("   ", "a@test.com", "Ann", "Lee"));

            verify(profileRepo, never()).insertIfAbsent(any(), any(), any(), any());
            verify(applicationEventPublisher, never()).publishEvent(any(ProfileUpsertedSpringEvent.class));
        }

        @Test
        @DisplayName("missing email key is skipped")
        void missingEmail_skips() {
            consumer.onUserRegistered(payload("u-1", null, "Ann", "Lee"));

            verify(profileRepo, never()).insertIfAbsent(any(), any(), any(), any());
            verify(applicationEventPublisher, never()).publishEvent(any(ProfileUpsertedSpringEvent.class));
        }

        @Test
        @DisplayName("blank email is skipped")
        void blankEmail_skips() {
            consumer.onUserRegistered(payload("u-1", "", "Ann", "Lee"));

            verify(profileRepo, never()).insertIfAbsent(any(), any(), any(), any());
            verify(applicationEventPublisher, never()).publishEvent(any(ProfileUpsertedSpringEvent.class));
        }

        @Test
        @DisplayName("both userId and email missing is skipped")
        void bothMissing_skips() {
            consumer.onUserRegistered(payload(null, null, "Ann", "Lee"));

            verify(profileRepo, never()).insertIfAbsent(any(), any(), any(), any());
            verify(applicationEventPublisher, never()).publishEvent(any(ProfileUpsertedSpringEvent.class));
        }
    }

    @Nested
    @DisplayName("Failure isolation")
    class FailureIsolation {

        @Test
        @DisplayName("repository exception is swallowed — no event published, no exception propagates")
        void repositoryThrows_isSwallowed_noPublish() {
            org.mockito.Mockito.doThrow(new RuntimeException("db down"))
                    .when(profileRepo).insertIfAbsent(eq("u-9"), eq("e@test.com"), any(), any());

            // must not propagate (method catches Exception and logs)
            consumer.onUserRegistered(payload("u-9", "e@test.com", "X", "Y"));

            verify(applicationEventPublisher, never()).publishEvent(any(ProfileUpsertedSpringEvent.class));
        }
    }
}
