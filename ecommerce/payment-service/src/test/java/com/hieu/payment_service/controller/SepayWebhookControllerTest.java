package com.hieu.payment_service.controller;

import com.hieu.payment_service.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Pure unit tests for {@link SepayWebhookController} — Apikey verification (constant-time),
 * orderId format guard, and dispatch to the payment service. No web layer / Spring context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SepayWebhookController (unit)")
class SepayWebhookControllerTest {

    private static final String API_KEY = "secret-key";
    private static final String VALID_ORDER = "ORD-20260101-000001";

    @Mock PaymentService paymentService;

    private SepayWebhookController secureController() {
        return new SepayWebhookController(paymentService, API_KEY, false);
    }

    private static Map<String, Object> payload(String description, Object id) {
        Map<String, Object> p = new HashMap<>();
        if (description != null) p.put("description", description);
        if (id != null) p.put("id", id);
        return p;
    }

    @Test
    @DisplayName("constructor fails fast when no api-key and not insecure")
    void constructor_requiresKey() {
        assertThat(catchThrowable(() -> new SepayWebhookController(paymentService, "", false)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("valid Apikey + well-formed orderId triggers auto-confirm")
    void validRequest_autoConfirms() {
        var resp = secureController().handleWebhook("Apikey " + API_KEY, payload(VALID_ORDER, "tx-9"));

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).containsEntry("status", "received");
        verify(paymentService).autoConfirmByOrderId(eq(VALID_ORDER), eq("tx-9"));
    }

    @Test
    @DisplayName("wrong Apikey is rejected with 401 and never touches the service")
    void wrongKey_unauthorized() {
        var resp = secureController().handleWebhook("Apikey wrong", payload(VALID_ORDER, "tx-9"));

        assertThat(resp.getStatusCode().value()).isEqualTo(401);
        assertThat(resp.getBody()).containsEntry("status", "unauthorized");
        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("missing description is skipped")
    void missingDescription_skipped() {
        var resp = secureController().handleWebhook("Apikey " + API_KEY, payload(null, "tx-9"));

        assertThat(resp.getBody()).containsEntry("status", "skipped");
        verify(paymentService, never()).autoConfirmByOrderId(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("malformed orderId is skipped (injection guard)")
    void malformedOrderId_skipped() {
        var resp = secureController().handleWebhook("Apikey " + API_KEY, payload("not-an-order", "tx-9"));

        assertThat(resp.getBody()).containsEntry("status", "skipped");
        verify(paymentService, never()).autoConfirmByOrderId(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("insecure mode skips Apikey verification")
    void insecureMode_skipsVerification() {
        var insecure = new SepayWebhookController(paymentService, "", true);

        var resp = insecure.handleWebhook(null, payload(VALID_ORDER, "tx-9"));

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(paymentService).autoConfirmByOrderId(eq(VALID_ORDER), eq("tx-9"));
    }

    // small local helper to avoid an extra import
    private static Throwable catchThrowable(Runnable r) {
        try {
            r.run();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }
}
