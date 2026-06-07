package com.hieu.order_service.interfaces.rest;

import com.hieu.common.security.AuthenticatedUser;
import com.hieu.order_service.application.command.returnrequest.ApproveReturnCommand;
import com.hieu.order_service.application.command.returnrequest.CompleteReturnCommand;
import com.hieu.order_service.application.command.returnrequest.RejectReturnCommand;
import com.hieu.order_service.application.command.returnrequest.RequestReturnCommand;
import com.hieu.order_service.application.dto.ReturnRequestDTO;
import com.hieu.order_service.application.handler.returnrequest.*;
import com.hieu.order_service.application.query.returnrequest.GetReturnRequestByIdQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link ReturnRequestController}'s body-parsing branches:
 * BigDecimal refund coercion (present vs. absent) and admin-flag derivation.
 */
@ExtendWith(MockitoExtension.class)
class ReturnRequestControllerTest {

    @Mock RequestReturnHandler requestReturnHandler;
    @Mock ApproveReturnHandler approveReturnHandler;
    @Mock RejectReturnHandler rejectReturnHandler;
    @Mock CompleteReturnHandler completeReturnHandler;
    @Mock GetReturnRequestByIdHandler getReturnRequestByIdHandler;
    @Mock ListUserReturnRequestsHandler listUserReturnRequestsHandler;

    ReturnRequestController controller;
    private static final ReturnRequestDTO DTO = new ReturnRequestDTO(
            "00000000-0000-0000-0000-000000000001", "00000000-0000-0000-0000-000000000002", "user-1", "reason", "REFUND", "PENDING", null, null, null,
            Instant.now(), Instant.now());

    @BeforeEach
    void setUp() {
        controller = new ReturnRequestController(
                requestReturnHandler, approveReturnHandler, rejectReturnHandler,
                completeReturnHandler, getReturnRequestByIdHandler, listUserReturnRequestsHandler);
    }

    private AuthenticatedUser user(String... roles) {
        return new AuthenticatedUser("user-1", "john", List.of(roles), List.of());
    }

    @Test
    @DisplayName("requestReturn forwards order id, user id and request payload")
    void requestReturn_buildsCommand() {
        when(requestReturnHandler.handle(any())).thenReturn(DTO);

        controller.requestReturn("00000000-0000-0000-0000-000000000002", new CreateReturnRequest("broken", "REFUND", "[]"), user("ROLE_USER"));

        var captor = ArgumentCaptor.forClass(RequestReturnCommand.class);
        verify(requestReturnHandler).handle(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(2L);
        assertThat(captor.getValue().userId()).isEqualTo("user-1");
        assertThat(captor.getValue().reason()).isEqualTo("broken");
        assertThat(captor.getValue().returnType()).isEqualTo("REFUND");
        assertThat(captor.getValue().images()).isEqualTo("[]");
    }

    @Test
    @DisplayName("getById sets isAdmin=true for an admin principal")
    void getById_admin() {
        when(getReturnRequestByIdHandler.handle(any())).thenReturn(DTO);

        controller.getById("00000000-0000-0000-0000-000000000007", user("ROLE_ADMIN"));

        var captor = ArgumentCaptor.forClass(GetReturnRequestByIdQuery.class);
        verify(getReturnRequestByIdHandler).handle(captor.capture());
        assertThat(captor.getValue().returnRequestId()).isEqualTo(7L);
        assertThat(captor.getValue().isAdmin()).isTrue();
    }

    @Test
    @DisplayName("approve parses a present refundAmount into a BigDecimal")
    void approve_withRefund() {
        when(approveReturnHandler.handle(any())).thenReturn(DTO);
        Map<String, Object> body = new HashMap<>();
        body.put("adminNote", "ok");
        body.put("refundAmount", "150.75");

        controller.approve("00000000-0000-0000-0000-000000000007", body);

        var captor = ArgumentCaptor.forClass(ApproveReturnCommand.class);
        verify(approveReturnHandler).handle(captor.capture());
        assertThat(captor.getValue().returnRequestId()).isEqualTo(7L);
        assertThat(captor.getValue().adminNote()).isEqualTo("ok");
        assertThat(captor.getValue().refundAmount()).isEqualByComparingTo("150.75");
    }

    @Test
    @DisplayName("approve leaves refundAmount null when absent from the body")
    void approve_withoutRefund() {
        when(approveReturnHandler.handle(any())).thenReturn(DTO);

        controller.approve("00000000-0000-0000-0000-000000000007", new HashMap<>());

        var captor = ArgumentCaptor.forClass(ApproveReturnCommand.class);
        verify(approveReturnHandler).handle(captor.capture());
        assertThat(captor.getValue().refundAmount()).isNull();
    }

    @Test
    @DisplayName("reject forwards the admin note")
    void reject_forwardsNote() {
        when(rejectReturnHandler.handle(any())).thenReturn(DTO);

        controller.reject("00000000-0000-0000-0000-000000000007", Map.of("adminNote", "not eligible"));

        var captor = ArgumentCaptor.forClass(RejectReturnCommand.class);
        verify(rejectReturnHandler).handle(captor.capture());
        assertThat(captor.getValue().returnRequestId()).isEqualTo(7L);
        assertThat(captor.getValue().adminNote()).isEqualTo("not eligible");
    }

    @Test
    @DisplayName("complete parses refundAmount when the body has it")
    void complete_withRefund() {
        when(completeReturnHandler.handle(any())).thenReturn(DTO);
        Map<String, Object> body = new HashMap<>();
        body.put("refundAmount", "42.00");

        controller.complete("00000000-0000-0000-0000-000000000007", body);

        var captor = ArgumentCaptor.forClass(CompleteReturnCommand.class);
        verify(completeReturnHandler).handle(captor.capture());
        assertThat(captor.getValue().refundAmount()).isEqualByComparingTo("42.00");
    }

    @Test
    @DisplayName("complete tolerates a null body (refundAmount null)")
    void complete_nullBody() {
        when(completeReturnHandler.handle(any())).thenReturn(DTO);

        controller.complete("00000000-0000-0000-0000-000000000007", null);

        var captor = ArgumentCaptor.forClass(CompleteReturnCommand.class);
        verify(completeReturnHandler).handle(captor.capture());
        assertThat(captor.getValue().returnRequestId()).isEqualTo(7L);
        assertThat(captor.getValue().refundAmount()).isNull();
    }
}
