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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link PaymentEventConsumer}. The reactive
 * {@link NotificationApplicationService} and the {@link UserProfileEmailResolver}
 * are mocked; no Kafka, Spring context, Mongo, Redis or gRPC are involved.
 *
 * <p>Behaviour verified via {@link ArgumentCaptor}: an IN_APP notification is always
 * sent with the right title/content/referenceType/referenceId; an EMAIL notification
 * is sent only when an email is resolvable (payload first, resolver fallback) and is
 * skipped entirely when no email can be resolved.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentEventConsumer (unit)")
class PaymentEventConsumerTest {

    @Mock NotificationApplicationService notificationService;
    @Mock UserProfileEmailResolver emailResolver;

    @InjectMocks PaymentEventConsumer consumer;

    @Captor ArgumentCaptor<SendNotificationRequest> requestCaptor;

    private final NotificationDTO dummyDto = new NotificationDTO(
            "id-1", "user-1", "IN_APP", null, "t", "c", "SENT",
            false, null, "PAYMENT", "pay-1", null, null, null);

    @BeforeEach
    void stubSend() {
        // Both branches call send(...).block(); always return a non-null Mono.
        when(notificationService.send(any(SendNotificationRequest.class)))
                .thenReturn(Mono.just(dummyDto));
    }

    private static Map<String, Object> completedPayload() {
        Map<String, Object> p = new HashMap<>();
        p.put("userId", "user-42");
        p.put("amount", "150000");
        p.put("paymentId", "pay-99");
        return p;
    }

    @Test
    @DisplayName("onPaymentCompleted: always sends IN_APP with correct title/content/reference")
    void completedSendsInApp() {
        Map<String, Object> payload = completedPayload();
        payload.put("email", "buyer@example.com"); // email present so the EMAIL branch also fires

        consumer.onPaymentCompleted(payload);

        verify(notificationService, times(2)).send(requestCaptor.capture());
        SendNotificationRequest inApp = requestCaptor.getAllValues().stream()
                .filter(r -> r.getType() == NotificationType.IN_APP)
                .findFirst().orElseThrow();

        assertThat(inApp.getUserId()).isEqualTo("user-42");
        assertThat(inApp.getTitle()).isEqualTo("Thanh toán 150000 VND thành công");
        assertThat(inApp.getContent()).isEqualTo("Thanh toán 150000 VND đã được xử lý thành công.");
        assertThat(inApp.getReferenceType()).isEqualTo("PAYMENT");
        assertThat(inApp.getReferenceId()).isEqualTo("pay-99");
        // IN_APP carries no channel.
        assertThat(inApp.getChannel()).isNull();
    }

    @Test
    @DisplayName("onPaymentCompleted: email in payload → EMAIL sent on that channel, resolver not consulted")
    void completedUsesPayloadEmail() {
        Map<String, Object> payload = completedPayload();
        payload.put("email", "payload@example.com");

        consumer.onPaymentCompleted(payload);

        verify(notificationService, times(2)).send(requestCaptor.capture());
        SendNotificationRequest email = requestCaptor.getAllValues().stream()
                .filter(r -> r.getType() == NotificationType.EMAIL)
                .findFirst().orElseThrow();

        assertThat(email.getChannel()).isEqualTo("payload@example.com");
        assertThat(email.getTitle()).isEqualTo("Thanh toán 150000 VND thành công");
        assertThat(email.getContent()).isEqualTo("Thanh toán 150000 VND đã được xử lý thành công.");
        assertThat(email.getReferenceType()).isEqualTo("PAYMENT");
        assertThat(email.getReferenceId()).isEqualTo("pay-99");
        // Payload email wins → resolver fallback never invoked.
        verifyNoInteractions(emailResolver);
    }

    @Test
    @DisplayName("onPaymentCompleted: no payload email → resolver fallback email used for EMAIL channel")
    void completedFallsBackToResolver() {
        Map<String, Object> payload = completedPayload(); // no "email" key
        when(emailResolver.lookupEmail("user-42")).thenReturn(Optional.of("resolved@example.com"));

        consumer.onPaymentCompleted(payload);

        verify(emailResolver).lookupEmail("user-42");
        verify(notificationService, times(2)).send(requestCaptor.capture());
        List<SendNotificationRequest> all = requestCaptor.getAllValues();

        SendNotificationRequest email = all.stream()
                .filter(r -> r.getType() == NotificationType.EMAIL)
                .findFirst().orElseThrow();
        assertThat(email.getChannel()).isEqualTo("resolved@example.com");

        // Exactly one IN_APP and one EMAIL.
        assertThat(all.stream().filter(r -> r.getType() == NotificationType.IN_APP).count()).isEqualTo(1);
        assertThat(all.stream().filter(r -> r.getType() == NotificationType.EMAIL).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("onPaymentCompleted: blank payload email + resolver empty → EMAIL skipped, only IN_APP")
    void completedSkipsEmailWhenUnresolvable() {
        Map<String, Object> payload = completedPayload();
        payload.put("email", "   "); // blank → forces resolver fallback
        when(emailResolver.lookupEmail("user-42")).thenReturn(Optional.empty());

        consumer.onPaymentCompleted(payload);

        verify(emailResolver).lookupEmail("user-42");
        verify(notificationService, times(1)).send(requestCaptor.capture());
        SendNotificationRequest only = requestCaptor.getValue();
        assertThat(only.getType()).isEqualTo(NotificationType.IN_APP);
    }

    @Test
    @DisplayName("onPaymentCompleted: missing keys default to empty strings (no NPE)")
    void completedHandlesMissingKeys() {
        Map<String, Object> payload = new HashMap<>(); // entirely empty
        when(emailResolver.lookupEmail("")).thenReturn(Optional.empty());

        consumer.onPaymentCompleted(payload);

        verify(notificationService, times(1)).send(requestCaptor.capture());
        SendNotificationRequest inApp = requestCaptor.getValue();
        assertThat(inApp.getType()).isEqualTo(NotificationType.IN_APP);
        assertThat(inApp.getUserId()).isEmpty();
        assertThat(inApp.getReferenceId()).isEmpty();
        assertThat(inApp.getTitle()).isEqualTo("Thanh toán  VND thành công");
    }

    @Test
    @DisplayName("onPaymentFailed: sends a single IN_APP notification, never an EMAIL, no resolver use")
    void failedSendsOnlyInApp() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", "user-7");
        payload.put("paymentId", "pay-7");

        consumer.onPaymentFailed(payload);

        verify(notificationService, times(1)).send(requestCaptor.capture());
        SendNotificationRequest req = requestCaptor.getValue();
        assertThat(req.getType()).isEqualTo(NotificationType.IN_APP);
        assertThat(req.getUserId()).isEqualTo("user-7");
        assertThat(req.getTitle()).isEqualTo("Thanh toán thất bại");
        assertThat(req.getContent())
                .isEqualTo("Giao dịch thanh toán của bạn không thành công. Vui lòng thử lại.");
        assertThat(req.getReferenceType()).isEqualTo("PAYMENT");
        assertThat(req.getReferenceId()).isEqualTo("pay-7");

        verify(notificationService, never()).send(org.mockito.ArgumentMatchers.argThat(
                r -> r != null && r.getType() == NotificationType.EMAIL));
        verifyNoInteractions(emailResolver);
    }
}
