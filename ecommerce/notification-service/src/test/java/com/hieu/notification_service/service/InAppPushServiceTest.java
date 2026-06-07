package com.hieu.notification_service.service;

import com.hieu.notification_service.dto.NotificationDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;

/**
 * Pure unit tests for the Reactor {@link Sinks.Many} based SSE push logic in
 * {@link InAppPushService}. No Spring context — we subscribe directly to the
 * returned {@link Flux} and exercise the sink emit / drop behaviour.
 */
@DisplayName("InAppPushService (unit)")
class InAppPushServiceTest {

    private final InAppPushService service = new InAppPushService();

    private static NotificationDTO dto(String id, String userId) {
        return new NotificationDTO(id, userId, "IN_APP", "in_app", "title", "content",
                "SENT", false, null, "PAYMENT", "ref-1", null, null, null);
    }

    @Test
    @DisplayName("push() to a user with no live subscriber is silently dropped (no error)")
    void pushWithoutSubscriberIsNoOp() {
        assertThatCode(() -> service.push("ghost-user", dto("n1", "ghost-user")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("subscribe() then push() delivers the DTO as a 'notification' ServerSentEvent")
    void subscribeThenPushDeliversEvent() {
        AtomicReference<ServerSentEvent<?>> received = new AtomicReference<>();

        // subscribe lazily allocates the per-user sink
        Flux<ServerSentEvent<?>> stream = service.subscribe("u1");
        Disposable sub = stream.subscribe(received::set);

        // give the subscription a moment to register before emitting
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            service.push("u1", dto("n1", "u1"));
            assertThat(received.get()).isNotNull();
        });

        ServerSentEvent<?> event = received.get();
        assertThat(event.event()).isEqualTo("notification");
        assertThat(event.data()).isInstanceOf(NotificationDTO.class);
        assertThat(((NotificationDTO) event.data()).id()).isEqualTo("n1");

        sub.dispose();
    }

    @Test
    @DisplayName("push() multicasts to every active subscriber of the same user")
    void pushMulticastsToAllSubscribers() {
        var firstTab = new CopyOnWriteArrayList<ServerSentEvent<?>>();
        var secondTab = new CopyOnWriteArrayList<ServerSentEvent<?>>();

        Disposable s1 = service.subscribe("u1").subscribe(firstTab::add);
        Disposable s2 = service.subscribe("u1").subscribe(secondTab::add);

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            service.push("u1", dto("n9", "u1"));
            assertThat(firstTab).isNotEmpty();
            assertThat(secondTab).isNotEmpty();
        });

        assertThat(((NotificationDTO) firstTab.get(0).data()).id()).isEqualTo("n9");
        assertThat(((NotificationDTO) secondTab.get(0).data()).id()).isEqualTo("n9");

        s1.dispose();
        s2.dispose();
    }

    @Test
    @DisplayName("push() is isolated per user — another user's subscriber receives nothing")
    void pushIsPerUserIsolated() {
        var otherUserEvents = new CopyOnWriteArrayList<ServerSentEvent<?>>();
        Disposable sub = service.subscribe("other").subscribe(otherUserEvents::add);

        // emit only to u1, who also has a live subscriber so the event isn't a no-op
        var u1Events = new CopyOnWriteArrayList<ServerSentEvent<?>>();
        Disposable u1Sub = service.subscribe("u1").subscribe(u1Events::add);

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            service.push("u1", dto("n1", "u1"));
            assertThat(u1Events).isNotEmpty();
        });

        // 'other' user must not have received u1's notification
        assertThat(otherUserEvents).isEmpty();

        sub.dispose();
        u1Sub.dispose();
    }
}
