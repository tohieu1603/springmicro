package com.hieu.payment_service.controller;

import com.hieu.common.api.ApiResponse;
import com.hieu.common.security.AuthenticatedUser;
import com.hieu.payment_service.config.PaymentMethodsProperties;
import com.hieu.payment_service.dto.ConfirmPaymentRequest;
import com.hieu.payment_service.dto.InitiatePaymentRequest;
import com.hieu.payment_service.dto.PageDTO;
import com.hieu.payment_service.dto.PaymentDTO;
import com.hieu.payment_service.dto.RefundRequest;
import com.hieu.payment_service.entity.PaymentMethodOverrideEntity;
import com.hieu.payment_service.repository.PaymentMethodOverrideRepository;
import com.hieu.payment_service.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link PaymentController} — exercises the controller's own branching:
 * admin-role detection, status codes, pagination passthrough, and the yaml+override merge
 * logic for the payment-method catalog. The service / repo / props are mocked; we assert the
 * returned {@link ResponseEntity} status and body. No MockMvc / Spring context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentController (unit)")
class PaymentControllerTest {

    @Mock PaymentService paymentService;
    @Mock PaymentMethodsProperties methodsProps;
    @Mock PaymentMethodOverrideRepository overrideRepo;

    private PaymentController controller() {
        return new PaymentController(paymentService, methodsProps, overrideRepo);
    }

    private static AuthenticatedUser user(String id, String... roles) {
        return new AuthenticatedUser(id, "u", List.of(roles), List.of());
    }

    private static PaymentDTO dto(String id) {
        return PaymentDTO.builder().id(id).status("PENDING").build();
    }

    private static PaymentMethodsProperties.Method method(
            String code, String name, boolean enabled) {
        return new PaymentMethodsProperties.Method(code, name, "desc-" + code, "icon-" + code, enabled);
    }

    // ── initiatePayment ────────────────────────────────────────────────

    @Test
    @DisplayName("initiatePayment returns 201 CREATED and delegates with the principal's userId")
    void initiate_created() {
        var req = new InitiatePaymentRequest();
        when(paymentService.initiatePayment("user-1", req)).thenReturn(dto("10"));

        var resp = controller().initiatePayment(req, user("user-1"));

        assertThat(resp.getStatusCode().value()).isEqualTo(201);
        assertThat(resp.getBody().success()).isTrue();
        assertThat(resp.getBody().message()).isEqualTo("Payment initiated");
        assertThat(resp.getBody().data().getId()).isEqualTo("10");
        verify(paymentService).initiatePayment("user-1", req);
    }

    // ── getPayment admin-flag branch ───────────────────────────────────

    @Test
    @DisplayName("getPayment passes isAdmin=false for a plain user")
    void getPayment_nonAdmin() {
        when(paymentService.getPayment("5", "user-1", false)).thenReturn(dto("5"));

        var resp = controller().getPayment("5", user("user-1", "ROLE_USER"));

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().data().getId()).isEqualTo("5");
        verify(paymentService).getPayment("5", "user-1", false);
    }

    @Test
    @DisplayName("getPayment passes isAdmin=true when principal has ROLE_ADMIN")
    void getPayment_roleAdmin() {
        when(paymentService.getPayment("5", "admin-1", true)).thenReturn(dto("5"));

        controller().getPayment("5", user("admin-1", "ROLE_ADMIN"));

        verify(paymentService).getPayment("5", "admin-1", true);
    }

    @Test
    @DisplayName("getPayment treats the bare 'ADMIN' role as admin too")
    void getPayment_bareAdminRole() {
        when(paymentService.getPayment("5", "admin-2", true)).thenReturn(dto("5"));

        controller().getPayment("5", user("admin-2", "ADMIN"));

        verify(paymentService).getPayment("5", "admin-2", true);
    }

    // ── pagination / passthrough endpoints ─────────────────────────────

    @Test
    @DisplayName("getMyPayments forwards page/size params to the service")
    void getMyPayments_forwardsPaging() {
        var page = PageDTO.<PaymentDTO>builder().content(List.of(dto("1"))).pageNumber(2).pageSize(25).build();
        when(paymentService.getMyPayments("user-1", 2, 25)).thenReturn(page);

        var resp = controller().getMyPayments(user("user-1"), 2, 25);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().data().getPageNumber()).isEqualTo(2);
        verify(paymentService).getMyPayments("user-1", 2, 25);
    }

    @Test
    @DisplayName("getPaymentByOrder delegates to the service")
    void getPaymentByOrder() {
        // Use ROLE_ADMIN so the access check in getPaymentByOrder is bypassed
        when(paymentService.getPaymentByOrder("ORD-9")).thenReturn(dto("3"));

        var resp = controller().getPaymentByOrder("ORD-9", user("user-1", "ROLE_ADMIN"));

        assertThat(resp.getBody().data().getId()).isEqualTo("3");
    }

    @Test
    @DisplayName("confirmPayment passes the transactionId from the body and returns confirmed message")
    void confirm() {
        var body = new ConfirmPaymentRequest();
        body.setTransactionId("tx-77");
        when(paymentService.confirmPayment("8", "user-1", "tx-77")).thenReturn(dto("8"));

        var resp = controller().confirmPayment("8", user("user-1"), body);

        assertThat(resp.getBody().message()).isEqualTo("Payment confirmed");
        verify(paymentService).confirmPayment("8", "user-1", "tx-77");
    }

    @Test
    @DisplayName("cancelPayment delegates and returns cancelled message")
    void cancel() {
        when(paymentService.cancelPayment("8", "user-1")).thenReturn(dto("8"));

        var resp = controller().cancelPayment("8", user("user-1"));

        assertThat(resp.getBody().message()).isEqualTo("Payment cancelled");
        verify(paymentService).cancelPayment("8", "user-1");
    }

    @Test
    @DisplayName("requestRefund passes the reason from the body")
    void requestRefund() {
        var body = new RefundRequest();
        body.setReason("damaged");
        when(paymentService.requestRefund("8", "user-1", "damaged")).thenReturn(dto("8"));

        var resp = controller().requestRefund("8", user("user-1"), body);

        assertThat(resp.getBody().message()).isEqualTo("Refund requested");
        verify(paymentService).requestRefund("8", "user-1", "damaged");
    }

    @Test
    @DisplayName("processRefund forwards amount+reason when a body is present")
    void processRefund_withBody() {
        var body = new RefundRequest();
        body.setReason("admin refund");
        body.setRefundAmount(new BigDecimal("12.50"));
        when(paymentService.processRefund("8", new BigDecimal("12.50"), "admin refund")).thenReturn(dto("8"));

        var resp = controller().processRefund("8", body);

        assertThat(resp.getBody().message()).isEqualTo("Refund processed");
        verify(paymentService).processRefund("8", new BigDecimal("12.50"), "admin refund");
    }

    @Test
    @DisplayName("processRefund passes nulls for amount/reason when body is absent (full refund)")
    void processRefund_noBody() {
        when(paymentService.processRefund("8", null, null)).thenReturn(dto("8"));

        var resp = controller().processRefund("8", null);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(paymentService).processRefund("8", null, null);
    }

    // ── listMethods (storefront merge/filter/sort) ─────────────────────

    @Test
    @DisplayName("listMethods returns empty list when no methods configured (null-safe)")
    void listMethods_nullSeed() {
        when(methodsProps.methods()).thenReturn(null);
        when(overrideRepo.findAll()).thenReturn(List.of());

        var resp = controller().listMethods();

        assertThat(resp.getBody().data()).isEmpty();
    }

    @Test
    @DisplayName("listMethods drops yaml-disabled methods and only returns enabled ones")
    void listMethods_filtersDisabled() {
        when(methodsProps.methods()).thenReturn(List.of(
                method("SEPAY", "SePay", true),
                method("COD", "Cash", false)));
        when(overrideRepo.findAll()).thenReturn(List.of());

        var resp = controller().listMethods();

        var data = resp.getBody().data();
        assertThat(data).hasSize(1);
        assertThat(data.get(0)).containsEntry("code", "SEPAY").containsEntry("enabled", true);
    }

    @Test
    @DisplayName("listMethods: override can disable a yaml-enabled method")
    void listMethods_overrideDisables() {
        when(methodsProps.methods()).thenReturn(List.of(method("MOMO", "MoMo", true)));
        var ov = new PaymentMethodOverrideEntity();
        ov.setCode("MOMO");
        ov.setEnabled(false);
        ov.setDisplayOrder(0);
        when(overrideRepo.findAll()).thenReturn(List.of(ov));

        var resp = controller().listMethods();

        assertThat(resp.getBody().data()).isEmpty();
    }

    @Test
    @DisplayName("listMethods: override can enable a yaml-disabled method and results are sorted by displayOrder")
    void listMethods_overrideEnablesAndSorts() {
        when(methodsProps.methods()).thenReturn(List.of(
                method("SEPAY", "SePay", true),      // yaml idx 0
                method("COD", "Cash", false)));       // yaml idx 1, disabled
        var enableCod = new PaymentMethodOverrideEntity();
        enableCod.setCode("COD");
        enableCod.setEnabled(true);
        enableCod.setDisplayOrder(1);
        var orderSepay = new PaymentMethodOverrideEntity();
        orderSepay.setCode("SEPAY");
        orderSepay.setEnabled(true);
        orderSepay.setDisplayOrder(5);
        when(overrideRepo.findAll()).thenReturn(List.of(enableCod, orderSepay));

        var resp = controller().listMethods();

        var data = resp.getBody().data();
        assertThat(data).hasSize(2);
        // COD has displayOrder 1, SEPAY has 5 -> COD first
        assertThat(data.get(0)).containsEntry("code", "COD").containsEntry("displayOrder", 1);
        assertThat(data.get(1)).containsEntry("code", "SEPAY").containsEntry("displayOrder", 5);
    }

    @Test
    @DisplayName("listMethods coerces null description/icon to empty strings")
    void listMethods_nullDescIcon() {
        when(methodsProps.methods()).thenReturn(List.of(
                new PaymentMethodsProperties.Method("SEPAY", "SePay", null, null, true)));
        when(overrideRepo.findAll()).thenReturn(List.of());

        var resp = controller().listMethods();

        assertThat(resp.getBody().data().get(0))
                .containsEntry("description", "")
                .containsEntry("icon", "");
    }

    // ── listMethodsAdmin (includes disabled) ───────────────────────────

    @Test
    @DisplayName("listMethodsAdmin includes disabled methods with override status applied")
    void listMethodsAdmin_includesDisabled() {
        when(methodsProps.methods()).thenReturn(List.of(
                method("SEPAY", "SePay", true),
                method("COD", "Cash", false)));
        var ov = new PaymentMethodOverrideEntity();
        ov.setCode("SEPAY");
        ov.setEnabled(false);
        ov.setDisplayOrder(3);
        when(overrideRepo.findAll()).thenReturn(List.of(ov));

        var resp = controller().listMethodsAdmin();

        var data = resp.getBody().data();
        assertThat(data).hasSize(2);
        assertThat(data.get(0)).containsEntry("code", "SEPAY")
                .containsEntry("enabled", false)        // override wins
                .containsEntry("displayOrder", 3);
        assertThat(data.get(1)).containsEntry("code", "COD")
                .containsEntry("enabled", false)        // yaml default, no override
                .containsEntry("displayOrder", 1);      // falls back to yaml index
    }

    // ── updateMethod (create-vs-update + partial body) ─────────────────

    @Test
    @DisplayName("updateMethod creates a new override row when none exists and applies the body")
    void updateMethod_create() {
        when(overrideRepo.findById("MOMO")).thenReturn(java.util.Optional.empty());

        var resp = controller().updateMethod("MOMO", Map.of("enabled", false, "displayOrder", 7));

        var captor = ArgumentCaptor.forClass(PaymentMethodOverrideEntity.class);
        verify(overrideRepo).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getCode()).isEqualTo("MOMO");
        assertThat(saved.isEnabled()).isFalse();
        assertThat(saved.getDisplayOrder()).isEqualTo(7);
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(resp.getBody().data())
                .containsEntry("code", "MOMO")
                .containsEntry("enabled", false)
                .containsEntry("displayOrder", 7);
    }

    @Test
    @DisplayName("updateMethod mutates an existing override and leaves untouched fields alone")
    void updateMethod_partialUpdate() {
        var existing = new PaymentMethodOverrideEntity();
        existing.setCode("SEPAY");
        existing.setEnabled(true);
        existing.setDisplayOrder(2);
        when(overrideRepo.findById("SEPAY")).thenReturn(java.util.Optional.of(existing));

        // body only toggles enabled; displayOrder must remain 2
        var resp = controller().updateMethod("SEPAY", Map.of("enabled", false));

        verify(overrideRepo).save(existing);
        assertThat(existing.isEnabled()).isFalse();
        assertThat(existing.getDisplayOrder()).isEqualTo(2);
        assertThat(resp.getBody().data())
                .containsEntry("enabled", false)
                .containsEntry("displayOrder", 2);
    }

    @Test
    @DisplayName("updateMethod with empty body leaves the entity unchanged but still persists")
    void updateMethod_emptyBody() {
        var existing = new PaymentMethodOverrideEntity();
        existing.setCode("COD");
        existing.setEnabled(true);
        existing.setDisplayOrder(4);
        when(overrideRepo.findById("COD")).thenReturn(java.util.Optional.of(existing));

        controller().updateMethod("COD", Map.of());

        verify(overrideRepo).save(existing);
        assertThat(existing.isEnabled()).isTrue();
        assertThat(existing.getDisplayOrder()).isEqualTo(4);
        verify(paymentService, never()).initiatePayment(eq("x"), org.mockito.ArgumentMatchers.any());
    }
}
