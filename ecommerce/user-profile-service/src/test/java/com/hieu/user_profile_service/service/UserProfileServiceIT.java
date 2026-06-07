package com.hieu.user_profile_service.service;

import com.hieu.user_profile_service.AbstractIntegrationTest;
import com.hieu.user_profile_service.dto.UpdateProfileRequest;
import com.hieu.user_profile_service.entity.UserProfileJpaEntity;
import com.hieu.user_profile_service.kafka.event.ProfileUpsertedSpringEvent;
import com.hieu.user_profile_service.repository.UserProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@DisplayName("UserProfileService — Integration")
@RecordApplicationEvents
class UserProfileServiceIT extends AbstractIntegrationTest {

    @Autowired
    private UserProfileService service;

    @Autowired
    private UserProfileRepository repo;

    @Autowired
    private ApplicationEvents applicationEvents;

    private static final String USER_ID = "svc-it-user-1";

    @BeforeEach
    void seed() {
        if (repo.findById(USER_ID).isEmpty()) {
            repo.insertIfAbsent(USER_ID, "svcit@test.com", "First", "Last");
        }
    }

    @AfterEach
    void cleanup() {
        repo.deleteById(USER_ID);
    }

    @Nested
    @DisplayName("updateProfile event publishing")
    class EventPublishing {

        @Test
        @DisplayName("updateProfile publishes ProfileUpsertedSpringEvent after DB commit")
        void updateProfile_publishesAfterCommit() {
            UpdateProfileRequest req = new UpdateProfileRequest();
            req.setFirstName("UpdatedFirst");
            req.setLastName("UpdatedLast");
            req.setPhone("+84999000001");

            service.updateProfile(USER_ID, req);

            long eventCount = applicationEvents
                    .stream(ProfileUpsertedSpringEvent.class)
                    .filter(e -> USER_ID.equals(e.userId()))
                    .count();
            assertThat(eventCount)
                    .as("One ProfileUpsertedSpringEvent should be published for userId=%s", USER_ID)
                    .isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("DB failure isolation")
    class DbFailure {

        @MockitoBean
        private UserProfileRepository mockRepo;

        @Test
        @DisplayName("repo.save throws — no ProfileUpsertedSpringEvent published")
        void updateProfile_dbFailure_doesNotPublish() {
            // Arrange: findById returns entity, save throws
            UserProfileJpaEntity entity = repo.findById(USER_ID)
                    .orElseGet(() -> {
                        UserProfileJpaEntity e = new UserProfileJpaEntity();
                        e.setUserId(USER_ID);
                        e.setEmail("svcit@test.com");
                        e.setFirstName("First");
                        e.setLastName("Last");
                        return e;
                    });

            org.mockito.Mockito.when(mockRepo.findById(USER_ID))
                    .thenReturn(java.util.Optional.of(entity));
            doThrow(new RuntimeException("DB down"))
                    .when(mockRepo).save(any());

            UpdateProfileRequest req = new UpdateProfileRequest();
            req.setFirstName("ShouldNotSave");

            assertThatThrownBy(() -> service.updateProfile(USER_ID, req))
                    .isInstanceOf(RuntimeException.class);

            long eventCount = applicationEvents
                    .stream(ProfileUpsertedSpringEvent.class)
                    .filter(e -> USER_ID.equals(e.userId()))
                    .count();
            assertThat(eventCount)
                    .as("No event should be published when save fails")
                    .isZero();
        }
    }
}
