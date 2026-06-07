package com.hieu.user_profile_service.domain;

import com.hieu.user_profile_service.entity.UserProfileJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserProfileJpaEntity — Unit")
class UserProfileTest {
    /**
     * Top-level smoke test — bắt buộc để Sonar rule java:S2187 nhận diện
     * class này có @Test (không tính các @Test bên trong @Nested).
     * Mọi test thật sự được tổ chức trong các @Nested class bên dưới.
     */
    @Test
    @DisplayName("class loads + JUnit discovers @Nested tests")
    void smokeTest_classDiscovered() {
        // Nếu tới được đây tức là JUnit có thể instantiate test class.
        // assertThat(this).isNotNull() được tự thực hiện ngầm.
    }


    private UserProfileJpaEntity newEntity(String userId, String email) {
        UserProfileJpaEntity e = new UserProfileJpaEntity();
        e.setUserId(userId);
        e.setEmail(email);
        e.setFirstName("Old");
        e.setLastName("Name");
        e.setPhone("+84900000000");
        OffsetDateTime now = OffsetDateTime.now();
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        e.setVersion(0L);
        return e;
    }

    @Nested
    @DisplayName("Update behaviour")
    class Update {

        @Test
        @DisplayName("setFirstName/setLastName/setPhone update the corresponding fields")
        void updateProfile_setsFields() {
            UserProfileJpaEntity e = newEntity("u1", "u1@test.com");

            e.setFirstName("Alice");
            e.setLastName("Wonderland");
            e.setPhone("+84911111111");

            assertThat(e.getFirstName()).isEqualTo("Alice");
            assertThat(e.getLastName()).isEqualTo("Wonderland");
            assertThat(e.getPhone()).isEqualTo("+84911111111");
        }

        @Test
        @DisplayName("updatedAt can be set to a later time to reflect an update")
        void updatedAt_canBeAdvanced() {
            UserProfileJpaEntity e = newEntity("u2", "u2@test.com");
            OffsetDateTime original = e.getUpdatedAt();

            OffsetDateTime later = original.plusSeconds(5);
            e.setUpdatedAt(later);

            assertThat(e.getUpdatedAt()).isAfter(original);
        }

        @Test
        @DisplayName("unmodified fields stay intact after partial update")
        void partialUpdate_doesNotTouchUnchangedFields() {
            UserProfileJpaEntity e = newEntity("u3", "u3@test.com");
            e.setAvatarUrl("https://cdn.example.com/avatar.jpg");

            e.setPhone("+84922222222");

            assertThat(e.getAvatarUrl()).isEqualTo("https://cdn.example.com/avatar.jpg");
            assertThat(e.getEmail()).isEqualTo("u3@test.com");
        }
    }

    @Nested
    @DisplayName("Field contract / nullable fields")
    class Validation {

        @Test
        @DisplayName("phone is optional — can be set to null without error")
        void phone_canBeNull() {
            UserProfileJpaEntity e = newEntity("u4", "u4@test.com");
            e.setPhone(null);

            assertThat(e.getPhone()).isNull();
        }

        @Test
        @DisplayName("email field is preserved as set")
        void email_preservedAsSet() {
            UserProfileJpaEntity e = newEntity("u5", "unique@domain.com");

            assertThat(e.getEmail()).isEqualTo("unique@domain.com");
        }

        @Test
        @DisplayName("createdAt and updatedAt are not null after explicit set")
        void timestamps_notNullAfterSet() {
            UserProfileJpaEntity e = newEntity("u6", "u6@test.com");

            assertThat(e.getCreatedAt()).isNotNull();
            assertThat(e.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("version starts at 0 for new entity")
        void version_startsAtZero() {
            UserProfileJpaEntity e = newEntity("u7", "u7@test.com");

            assertThat(e.getVersion()).isEqualTo(0L);
        }
    }
}
