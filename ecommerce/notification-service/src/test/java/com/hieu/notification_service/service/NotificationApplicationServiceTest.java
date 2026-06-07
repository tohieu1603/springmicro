package com.hieu.notification_service.service;

import com.hieu.notification_service.dto.NotificationDTO;
import com.hieu.notification_service.dto.SendNotificationRequest;
import com.hieu.notification_service.entity.NotificationDocument;
import com.hieu.notification_service.entity.NotificationStatus;
import com.hieu.notification_service.entity.NotificationType;
import com.hieu.notification_service.exception.AccessDeniedException;
import com.hieu.notification_service.exception.NotificationNotFoundException;
import com.hieu.notification_service.repository.NotificationRepository;
import com.mongodb.client.result.UpdateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for the reactive {@link NotificationApplicationService}. The reactive
 * repository, ReactiveMongoTemplate, email and in-app push services are mocked; Mono results
 * are asserted with {@code .block()} (no MongoDB / Spring context).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationApplicationService (unit)")
class NotificationApplicationServiceTest {

    @Mock NotificationRepository repository;
    @Mock ReactiveMongoTemplate mongoTemplate;
    @Mock EmailService emailService;
    @Mock InAppPushService inAppPushService;

    NotificationApplicationService service;

    @BeforeEach
    void setup() {
        service = new NotificationApplicationService(repository, mongoTemplate, emailService, inAppPushService);
    }

    private static NotificationDocument doc(String id, String userId, String status) {
        return NotificationDocument.builder()
                .id(id).userId(userId).type("IN_APP")
                .title("title").content("content").status(status)
                .referenceType("PAYMENT").referenceId("ref-1")
                .build();
    }

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("returns the notification for its owner")
        void owner() {
            when(repository.findById("n1")).thenReturn(Mono.just(doc("n1", "u1", "SENT")));
            NotificationDTO dto = service.getById("n1", "u1", false).block();
            assertThat(dto).isNotNull();
            assertThat(dto.id()).isEqualTo("n1");
        }

        @Test
        @DisplayName("denies a different non-admin user")
        void accessDenied() {
            when(repository.findById("n1")).thenReturn(Mono.just(doc("n1", "u1", "SENT")));
            assertThatThrownBy(() -> service.getById("n1", "intruder", false).block())
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("an admin may read another user's notification")
        void admin() {
            when(repository.findById("n1")).thenReturn(Mono.just(doc("n1", "u1", "SENT")));
            assertThat(service.getById("n1", "admin", true).block()).isNotNull();
        }

        @Test
        @DisplayName("throws when the notification does not exist")
        void notFound() {
            when(repository.findById("missing")).thenReturn(Mono.empty());
            assertThatThrownBy(() -> service.getById("missing", "u1", false).block())
                    .isInstanceOf(NotificationNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("markAsRead()")
    class MarkAsRead {

        @Test
        @DisplayName("marks the owner's notification read and transitions SENT→READ")
        void owner() {
            when(repository.findById("n1")).thenReturn(Mono.just(doc("n1", "u1", "SENT")));
            when(repository.save(any(NotificationDocument.class)))
                    .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            NotificationDTO dto = service.markAsRead("n1", "u1").block();

            assertThat(dto).isNotNull();
            assertThat(dto.isRead()).isTrue();
            assertThat(dto.status()).isEqualTo(NotificationStatus.READ.name());
        }

        @Test
        @DisplayName("denies marking another user's notification")
        void accessDenied() {
            when(repository.findById("n1")).thenReturn(Mono.just(doc("n1", "u1", "SENT")));
            assertThatThrownBy(() -> service.markAsRead("n1", "intruder").block())
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("throws when the notification does not exist")
        void notFound() {
            when(repository.findById("missing")).thenReturn(Mono.empty());
            assertThatThrownBy(() -> service.markAsRead("missing", "u1").block())
                    .isInstanceOf(NotificationNotFoundException.class);
        }
    }

    @Test
    @DisplayName("getUnreadCount() delegates to the reactive count query")
    void getUnreadCount() {
        when(repository.countByUserIdAndIsRead("u1", false)).thenReturn(Mono.just(7L));
        assertThat(service.getUnreadCount("u1").block()).isEqualTo(7L);
    }

    @Test
    @DisplayName("markAllReadForUser() returns the modified count from the bulk update")
    void markAllReadForUser() {
        UpdateResult result = org.mockito.Mockito.mock(UpdateResult.class);
        when(result.getModifiedCount()).thenReturn(3L);
        when(mongoTemplate.updateMulti(any(Query.class), any(UpdateDefinition.class), eq(NotificationDocument.class)))
                .thenReturn(Mono.just(result));

        assertThat(service.markAllReadForUser("u1").block()).isEqualTo(3L);
    }

    @Test
    @DisplayName("send() IN_APP persists, marks SENT and pushes to the in-app sink")
    void send_inApp() {
        when(repository.save(any(NotificationDocument.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        var req = SendNotificationRequest.builder()
                .userId("u1").type(NotificationType.IN_APP)
                .title("title").content("content")
                .referenceType("PAYMENT").referenceId("ref-1")
                .build();

        NotificationDTO dto = service.send(req).block();

        assertThat(dto).isNotNull();
        assertThat(dto.status()).isEqualTo(NotificationStatus.SENT.name());
        verify(inAppPushService).push(eq("u1"), any(NotificationDTO.class));
    }
}
