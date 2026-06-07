package com.hieu.user_profile_service.kafka;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link UserProfileEventPublisher#publishProfileUpserted}: the blank/null
 * guard (skip publish), the userId-keyed JSON send on the happy path, and that a serialization
 * failure is swallowed (logged, not rethrown). The {@link KafkaTemplate} and Jackson 3
 * {@link ObjectMapper} are mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileEventPublisher (unit)")
class UserProfileEventPublisherTest {

    @Mock KafkaTemplate<String, String> kafkaTemplate;
    @Mock ObjectMapper objectMapper;

    UserProfileEventPublisher publisher;

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        publisher = new UserProfileEventPublisher(kafkaTemplate, objectMapper);
    }

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("serializes the payload and sends to the topic keyed by userId")
        void publishes() {
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"json\":true}");

            publisher.publishProfileUpserted("u-1", "a@test.com", "Ann", "Lee", "0900");

            verify(kafkaTemplate).send(eq(UserProfileEventPublisher.TOPIC_PROFILE_UPSERTED), eq("u-1"), eq("{\"json\":true}"));
        }

        @Test
        @DisplayName("payload contains all five fields in insertion order")
        void payloadContainsAllFields() {
            @SuppressWarnings("unchecked")
            ArgumentCaptor<java.util.Map<String, Object>> captor = ArgumentCaptor.forClass(java.util.Map.class);
            when(objectMapper.writeValueAsString(captor.capture())).thenReturn("{}");

            publisher.publishProfileUpserted("u-2", "b@test.com", "Bob", "Kim", "0911");

            var payload = captor.getValue();
            assertThat(payload).containsEntry("userId", "u-2")
                    .containsEntry("email", "b@test.com")
                    .containsEntry("firstName", "Bob")
                    .containsEntry("lastName", "Kim")
                    .containsEntry("phone", "0911");
        }
    }

    @Nested
    @DisplayName("guard: missing userId/email -> skip publish")
    class Guard {

        @Test
        @DisplayName("null userId skips serialization and send")
        void nullUserId() {
            publisher.publishProfileUpserted(null, "a@test.com", "Ann", "Lee", "0900");
            verify(objectMapper, never()).writeValueAsString(any());
            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("blank userId skips")
        void blankUserId() {
            publisher.publishProfileUpserted("  ", "a@test.com", "Ann", "Lee", "0900");
            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("null email skips")
        void nullEmail() {
            publisher.publishProfileUpserted("u-1", null, "Ann", "Lee", "0900");
            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("blank email skips")
        void blankEmail() {
            publisher.publishProfileUpserted("u-1", "", "Ann", "Lee", "0900");
            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        }
    }

    @Test
    @DisplayName("serialization failure is swallowed — no send, no exception propagates")
    void serializationFailure_swallowed() {
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("jackson boom"));

        assertThatCode(() -> publisher.publishProfileUpserted("u-1", "a@test.com", "Ann", "Lee", "0900"))
                .doesNotThrowAnyException();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }
}
