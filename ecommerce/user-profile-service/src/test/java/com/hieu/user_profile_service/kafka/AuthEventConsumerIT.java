package com.hieu.user_profile_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hieu.user_profile_service.AbstractIntegrationTest;
import com.hieu.user_profile_service.repository.UserProfileRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

@DisplayName("AuthEventConsumer — Integration")
class AuthEventConsumerIT extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private UserProfileRepository repo;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TOPIC = "auth.user-registered";

    @AfterEach
    void cleanup() {
        repo.deleteById("auth-evt-user-1");
        repo.deleteById("auth-evt-user-idem");
        repo.deleteById("auth-evt-user-pub-fail");
    }

    @Nested
    @DisplayName("Successful registration")
    class SuccessPath {

        @Test
        @DisplayName("auth.user-registered inserts a new user_profiles row")
        void authUserRegistered_insertsProfile() throws Exception {
            Map<String, Object> payload = Map.of(
                    "userId", "auth-evt-user-1",
                    "email", "authevt1@test.com",
                    "firstName", "Auth",
                    "lastName", "One"
            );
            kafkaTemplate.send(TOPIC, objectMapper.writeValueAsString(payload));

            Awaitility.await()
                    .atMost(Duration.ofSeconds(30))
                    .pollDelay(Duration.ofSeconds(1))
                    .untilAsserted(() ->
                            assertThat(repo.findById("auth-evt-user-1"))
                                    .as("Profile row should exist after auth.user-registered event")
                                    .isPresent());
        }

        @Test
        @DisplayName("duplicate auth.user-registered events produce exactly 1 row (idempotent)")
        void authUserRegistered_duplicateEvent_idempotent() throws Exception {
            Map<String, Object> payload = Map.of(
                    "userId", "auth-evt-user-idem",
                    "email", "authevtidem@test.com",
                    "firstName", "Idem",
                    "lastName", "Potent"
            );
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(TOPIC, json);
            kafkaTemplate.send(TOPIC, json);

            Awaitility.await()
                    .atMost(Duration.ofSeconds(30))
                    .pollDelay(Duration.ofSeconds(1))
                    .untilAsserted(() ->
                            assertThat(repo.findById("auth-evt-user-idem")).isPresent());

            // ON CONFLICT DO NOTHING guarantees exactly 1 row
            long count = repo.findAll().stream()
                    .filter(p -> "auth-evt-user-idem".equals(p.getUserId()))
                    .count();
            assertThat(count).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Publisher failure isolation")
    class PublisherFailure {

        // NOTE: UserProfileEventPublisher.publishProfileUpserted is called inside a
        // try-catch in ProfileUpsertedListener (AFTER_COMMIT). The consumer's @Transactional
        // scope has already committed, so a publisher exception cannot roll back the DB insert.
        // We verify the DB row still exists even when the publisher throws.

        @MockitoBean
        private UserProfileEventPublisher mockPublisher;

        @Test
        @DisplayName("DB insert succeeds even when Kafka publisher throws — row persists")
        void authUserRegistered_dbInsertSucceeds_kafkaPublishFailure_doesNotRollback()
                throws Exception {
            doThrow(new RuntimeException("Kafka broker down"))
                    .when(mockPublisher)
                    .publishProfileUpserted(anyString(), anyString(), any(), any(), any());

            Map<String, Object> payload = Map.of(
                    "userId", "auth-evt-user-pub-fail",
                    "email", "authevtpubfail@test.com",
                    "firstName", "Pub",
                    "lastName", "Fail"
            );
            kafkaTemplate.send(TOPIC, objectMapper.writeValueAsString(payload));

            Awaitility.await()
                    .atMost(Duration.ofSeconds(30))
                    .pollDelay(Duration.ofSeconds(1))
                    .untilAsserted(() ->
                            assertThat(repo.findById("auth-evt-user-pub-fail"))
                                    .as("DB row must exist despite publisher throwing")
                                    .isPresent());
        }
    }
}
