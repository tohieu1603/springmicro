package com.hieu.notification_service.kafka;

import com.hieu.notification_service.dto.NotificationDTO;
import com.hieu.notification_service.dto.SendNotificationRequest;
import com.hieu.notification_service.entity.NotificationType;
import com.hieu.notification_service.service.NotificationApplicationService;
import com.hieu.notification_service.service.UserProfileEmailResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link ShippingEventConsumer}. The reactive
 * {@link NotificationApplicationService} and the {@link UserProfileEmailResolver}
 * are mocked; no Kafka, Spring context, Mongo, Redis or gRPC are involved.
 *
 * <p>Behaviour verified via {@link ArgumentCaptor}: an IN_APP notification is always
 * sent with the right title/content/referenceType/referenceId; an EMAIL notification
 * is sent only when an email is resolvable (payload first, resolver fallback) and is
 * skipped entirely when no email can be resolved.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ShippingEventConsumer (unit)")
class ShippingEventConsumerTest {

    @Mock NotificationApplicationService notificationService;
    @Mock UserProfileEmailResolver emailResolver;

    @InjectMocks ShippingEventConsumer consumer;

    @Captor ArgumentCaptor<SendNotificationRequest> requestCaptor;

    private final NotificationDTO dummyDto = new NotificationDTO(
            "id-1", "user-1", "IN_APP", null, "t", "c", "SENT",
            false, null, "SHIPPING", "ORD-1", null, null, null);

    @BeforeEach
    void stubSend() {
        when(notificationService.send(any(SendNotificationRequest.class)))
                .thenReturn(Mono.just(dummyDto));
    }

    private static Map<String, Object> basePayload() {
        Map<String, Object> p = new HashMap<>();
        p.put("userId", "user-42");
        p.put("orderNumber", "ORD-1001");
        p.put("status", "SHIPPED");
        return p;
    }

    @Test
    @DisplayName("onShippingStatusChanged: always sends IN_APP with title==content built from order+status")
    void sendsInApp() {
        Map<String, Object> payload = basePayload();
        payload.put("email", "buyer@example.com");

        consumer.onShippingStatusChanged(payload);

        verify(notificationService, times(2)).send(requestCaptor.capture());
        SendNotificationRequest inApp = requestCaptor.getAllValues().stream()
                .filter(r -> r.getType() == NotificationType.IN_APP)
                .findFirst().orElseThrow();

        String expectedTitle = "Đơn ORD-1001 đang ở trạng thái SHIPPED";
        assertThat(inApp.getUserId()).isEqualTo("user-42");
        assertThat(inApp.getTitle()).isEqualTo(expectedTitle);
        // For shipping, content mirrors the title.
        assertThat(inApp.getContent()).isEqualTo(expectedTitle);
        assertThat(inApp.getReferenceType()).isEqualTo("SHIPPING");
        assertThat(inApp.getReferenceId()).isEqualTo("ORD-1001");
        assertThat(inApp.getChannel()).isNull();
    }

    @Test
    @DisplayName("onShippingStatusChanged: payload email → EMAIL on that channel, resolver untouched")
    void usesPayloadEmail() {
        Map<String, Object> payload = basePayload();
        payload.put("email", "payload@example.com");

        consumer.onShippingStatusChanged(payload);

        verify(notificationService, times(2)).send(requestCaptor.capture());
        SendNotificationRequest email = requestCaptor.getAllValues().stream()
                .filter(r -> r.getType() == NotificationType.EMAIL)
                .findFirst().orElseThrow();

        assertThat(email.getChannel()).isEqualTo("payload@example.com");
        assertThat(email.getTitle()).isEqualTo("Đơn ORD-1001 đang ở trạng thái SHIPPED");
        assertThat(email.getContent()).isEqualTo("Đơn ORD-1001 đang ở trạng thái SHIPPED");
        assertThat(email.getReferenceType()).isEqualTo("SHIPPING");
        assertThat(email.getReferenceId()).isEqualTo("ORD-1001");
        verifyNoInteractions(emailResolver);
    }

    @Test
    @DisplayName("onShippingStatusChanged: no payload email → resolver fallback used for EMAIL channel")
    void fallsBackToResolver() {
        Map<String, Object> payload = basePayload(); // no email key
        when(emailResolver.lookupEmail("user-42")).thenReturn(Optional.of("resolved@example.com"));

        consumer.onShippingStatusChanged(payload);

        verify(emailResolver).lookupEmail("user-42");
        verify(notificationService, times(2)).send(requestCaptor.capture());
        List<SendNotificationRequest> all = requestCaptor.getAllValues();

        SendNotificationRequest email = all.stream()
                .filter(r -> r.getType() == NotificationType.EMAIL)
                .findFirst().orElseThrow();
        assertThat(email.getChannel()).isEqualTo("resolved@example.com");

        assertThat(all.stream().filter(r -> r.getType() == NotificationType.IN_APP).count()).isEqualTo(1);
        assertThat(all.stream().filter(r -> r.getType() == NotificationType.EMAIL).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("onShippingStatusChanged: blank payload email + resolver empty → EMAIL skipped, only IN_APP")
    void skipsEmailWhenUnresolvable() {
        Map<String, Object> payload = basePayload();
        payload.put("email", ""); // blank → resolver fallback
        when(emailResolver.lookupEmail("user-42")).thenReturn(Optional.empty());

        consumer.onShippingStatusChanged(payload);

        verify(emailResolver).lookupEmail("user-42");
        verify(notificationService, times(1)).send(requestCaptor.capture());
        SendNotificationRequest only = requestCaptor.getValue();
        assertThat(only.getType()).isEqualTo(NotificationType.IN_APP);
    }

    @Test
    @DisplayName("onShippingStatusChanged: missing keys default to empty strings (no NPE)")
    void handlesMissingKeys() {
        Map<String, Object> payload = new HashMap<>();
        when(emailResolver.lookupEmail("")).thenReturn(Optional.empty());

        consumer.onShippingStatusChanged(payload);

        verify(notificationService, times(1)).send(requestCaptor.capture());
        SendNotificationRequest inApp = requestCaptor.getValue();
        assertThat(inApp.getType()).isEqualTo(NotificationType.IN_APP);
        assertThat(inApp.getUserId()).isEmpty();
        assertThat(inApp.getReferenceId()).isEmpty();
        assertThat(inApp.getTitle()).isEqualTo("Đơn  đang ở trạng thái ");
    }
}
