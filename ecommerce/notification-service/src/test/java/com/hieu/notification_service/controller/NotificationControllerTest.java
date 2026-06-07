package com.hieu.notification_service.controller;

import com.hieu.common.security.AuthenticatedUser;
import com.hieu.notification_service.dto.CursorPageDTO;
import com.hieu.notification_service.dto.NotificationDTO;
import com.hieu.notification_service.dto.PageDTO;
import com.hieu.notification_service.dto.SendNotificationRequest;
import com.hieu.notification_service.entity.NotificationType;
import com.hieu.notification_service.service.InAppPushService;
import com.hieu.notification_service.service.NotificationApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for the WebFlux {@link NotificationController}. The application
 * service and in-app push service are mocked; we assert the controller's real
 * branching — CREATED status mapping, principal {@code userId} extraction, the
 * {@code isAdmin} flag derived from roles, and the unread-count map wrapping —
 * by blocking on the returned reactive types. No MockMvc / WebTestClient / context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController (unit)")
class NotificationControllerTest {

    @Mock NotificationApplicationService service;
    @Mock InAppPushService inAppPushService;

    NotificationController controller;

    @BeforeEach
    void setup() {
        controller = new NotificationController(service, inAppPushService);
    }

    private static NotificationDTO dto(String id, String userId) {
        return new NotificationDTO(id, userId, "IN_APP", "in_app", "title", "content",
                "SENT", false, null, "PAYMENT", "ref-1", null, null, null);
    }

    private static AuthenticatedUser user(String userId, List<String> roles) {
        return new AuthenticatedUser(userId, "alice", roles, List.of());
    }

    @Test
    @DisplayName("send() maps the service result to HTTP 201 CREATED")
    void send_returnsCreated() {
        var req = SendNotificationRequest.builder()
                .userId("u1").type(NotificationType.IN_APP)
                .title("title").content("content")
                .referenceType("PAYMENT").referenceId("ref-1")
                .build();
        when(service.send(req)).thenReturn(Mono.just(dto("n1", "u1")));

        ResponseEntity<NotificationDTO> resp = controller.send(req).block();

        assertThat(resp).isNotNull();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().id()).isEqualTo("n1");
    }

    @Test
    @DisplayName("getMyNotifications() forwards the principal userId, page and size")
    void getMyNotifications_forwardsPrincipal() {
        var page = new PageDTO<>(List.of(dto("n1", "u1")), 0, 20, 1L, 1, true);
        when(service.getMyNotifications("u1", 2, 50)).thenReturn(Mono.just(page));

        var result = controller.getMyNotifications(user("u1", List.of()), 2, 50).block();

        assertThat(result).isSameAs(page);
        verify(service).getMyNotifications("u1", 2, 50);
    }

    @Test
    @DisplayName("getMyFeed() forwards the principal userId, cursor and size")
    void getMyFeed_forwardsCursor() {
        var feed = new CursorPageDTO<>(List.of(dto("n1", "u1")), "cursor-2", 1, true);
        when(service.getMyFeed("u1", "cursor-1", 20)).thenReturn(Mono.just(feed));

        var result = controller.getMyFeed(user("u1", List.of()), "cursor-1", 20).block();

        assertThat(result).isSameAs(feed);
        verify(service).getMyFeed("u1", "cursor-1", 20);
    }

    @Test
    @DisplayName("getUnreadCount() wraps the count in a {count: n} map")
    void getUnreadCount_wrapsInMap() {
        when(service.getUnreadCount("u1")).thenReturn(Mono.just(5L));

        Map<String, Long> result = controller.getUnreadCount(user("u1", List.of())).block();

        assertThat(result).containsEntry("count", 5L);
    }

    @Test
    @DisplayName("getById() passes isAdmin=true when the principal holds ROLE_ADMIN")
    void getById_adminFlagTrue() {
        when(service.getById("n1", "admin-id", true)).thenReturn(Mono.just(dto("n1", "other")));

        var result = controller.getById("n1", user("admin-id", List.of("ROLE_ADMIN"))).block();

        assertThat(result).isNotNull();
        verify(service).getById("n1", "admin-id", true);
    }

    @Test
    @DisplayName("getById() passes isAdmin=false for a plain user")
    void getById_adminFlagFalse() {
        when(service.getById("n1", "u1", false)).thenReturn(Mono.just(dto("n1", "u1")));

        controller.getById("n1", user("u1", List.of("ROLE_USER"))).block();

        verify(service).getById("n1", "u1", false);
    }

    @Test
    @DisplayName("markAsRead() forwards id and principal userId")
    void markAsRead_forwards() {
        when(service.markAsRead("n1", "u1")).thenReturn(Mono.just(dto("n1", "u1")));

        var result = controller.markAsRead("n1", user("u1", List.of())).block();

        assertThat(result).isNotNull();
        verify(service).markAsRead("n1", "u1");
    }

    @Test
    @DisplayName("markAllAsRead() delegates to the service and completes with Void")
    void markAllAsRead_completes() {
        when(service.markAllReadForUser("u1")).thenReturn(Mono.just(4L));

        // .block() on Mono<Void> returns null on completion; assert no error
        controller.markAllAsRead(user("u1", List.of())).block();

        verify(service).markAllReadForUser("u1");
    }

    @Test
    @DisplayName("stream() delegates SSE to InAppPushService.subscribe(userId)")
    void stream_delegatesToPushService() {
        ServerSentEvent<?> event = ServerSentEvent.builder().event("notification").data((Object) dto("n1", "u1")).build();
        when(inAppPushService.subscribe("u1")).thenReturn(Flux.just(event));

        List<ServerSentEvent<?>> events = controller.stream(user("u1", List.of())).collectList().block();

        assertThat(events).containsExactly(event);
        verify(inAppPushService).subscribe("u1");
        verifyNoInteractions(service);
    }
}
