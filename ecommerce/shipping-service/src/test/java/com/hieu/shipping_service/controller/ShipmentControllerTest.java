package com.hieu.shipping_service.controller;

import com.hieu.common.api.ApiResponse;
import com.hieu.common.security.AuthenticatedUser;
import com.hieu.shipping_service.dto.*;
import com.hieu.shipping_service.service.ShipmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link ShipmentController}. The service is mocked; we assert the
 * controller wires principal fields (userId, ROLE_ADMIN) and request bodies into the
 * service correctly and returns the right HTTP status + envelope. No MockMvc, no Spring.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ShipmentController (unit)")
class ShipmentControllerTest {

    @Mock ShipmentService service;
    @InjectMocks ShipmentController controller;

    private static ShipmentDTO dto(String id) {
        return new ShipmentDTO(id, "ORD-1", "u1", "GHTK", "TRK-1", "PENDING",
                "Recipient", "0900000000", "123 Street", "Ward", "District", "HCM", "Vietnam",
                null, null, "note", Instant.now(), Instant.now(), 0L);
    }

    private static AuthenticatedUser user(String userId, boolean admin) {
        return new AuthenticatedUser(userId, "user", admin ? List.of("ROLE_ADMIN") : List.of("ROLE_USER"), List.of());
    }

    @Test
    @DisplayName("createShipment returns 201 CREATED with the created shipment")
    void createShipment_created() {
        var req = new CreateShipmentRequest("ORD-1", "u1", "GHTK", "R", "0900000000",
                "123", null, null, "HCM", "Vietnam", null);
        when(service.createShipment(req)).thenReturn(dto("uuid-1"));

        ResponseEntity<ApiResponse<ShipmentDTO>> resp = controller.createShipment(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().success()).isTrue();
        assertThat(resp.getBody().data().id()).isEqualTo("uuid-1");
        assertThat(resp.getBody().message()).isEqualTo("Shipment created");
    }

    @Test
    @DisplayName("createShipmentInternal returns 201 CREATED with internal message")
    void createShipmentInternal_created() {
        var req = new CreateShipmentRequest("ORD-1", "u1", "GHTK", "R", "0900000000",
                "123", null, null, "HCM", "Vietnam", null);
        when(service.createShipment(req)).thenReturn(dto("uuid-2"));

        var resp = controller.createShipmentInternal(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().message()).isEqualTo("Shipment created (internal)");
    }

    @Test
    @DisplayName("getShipment passes userId and admin=false for a regular user")
    void getShipment_regularUser() {
        when(service.getShipmentForUser("uuid-1", "u1", false)).thenReturn(dto("uuid-1"));

        var resp = controller.getShipment("uuid-1", user("u1", false));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().data().id()).isEqualTo("uuid-1");
        verify(service).getShipmentForUser("uuid-1", "u1", false);
    }

    @Test
    @DisplayName("getShipment passes admin=true when principal has ROLE_ADMIN")
    void getShipment_admin() {
        when(service.getShipmentForUser("uuid-1", "admin-id", true)).thenReturn(dto("uuid-1"));

        controller.getShipment("uuid-1", user("admin-id", true));

        verify(service).getShipmentForUser("uuid-1", "admin-id", true);
    }

    @Test
    @DisplayName("getByOrder forwards orderId + principal + admin flag")
    void getByOrder() {
        when(service.getShipmentByOrderForUser("ORD-1", "u1", false)).thenReturn(dto("uuid-1"));

        var resp = controller.getByOrder("ORD-1", user("u1", false));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service).getShipmentByOrderForUser("ORD-1", "u1", false);
    }

    @Test
    @DisplayName("getTracking returns public tracking info")
    void getTracking() {
        var info = new TrackingInfoDTO("TRK-1", "GHTK", "IN_TRANSIT", "HCM", null, null);
        when(service.getTrackingInfo("TRK-1")).thenReturn(info);

        var resp = controller.getTracking("TRK-1");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().data().trackingNumber()).isEqualTo("TRK-1");
    }

    @Test
    @DisplayName("getMyShipments uses the principal's userId")
    void getMyShipments() {
        when(service.getMyShipments("u1")).thenReturn(List.of(dto("uuid-1"), dto("uuid-2")));

        var resp = controller.getMyShipments(user("u1", false));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().data()).hasSize(2);
        verify(service).getMyShipments("u1");
    }

    @Test
    @DisplayName("updateStatus forwards status + notes and returns OK")
    void updateStatus() {
        when(service.updateStatus("uuid-1", "PICKED_UP", "picked")).thenReturn(dto("uuid-1"));

        var resp = controller.updateStatus("uuid-1", new UpdateStatusRequest("PICKED_UP", "picked"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().message()).isEqualTo("Status updated");
        verify(service).updateStatus("uuid-1", "PICKED_UP", "picked");
    }

    @Test
    @DisplayName("assignTracking forwards carrier + trackingNumber")
    void assignTracking() {
        when(service.assignTracking("uuid-1", "GHTK", "TRK-1")).thenReturn(dto("uuid-1"));

        var resp = controller.assignTracking("uuid-1", new AssignTrackingRequest("GHTK", "TRK-1"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().message()).isEqualTo("Tracking assigned");
        verify(service).assignTracking("uuid-1", "GHTK", "TRK-1");
    }

    @Test
    @DisplayName("setEstimatedDelivery forwards the date")
    void setEstimatedDelivery() {
        var when = Instant.parse("2026-06-10T00:00:00Z");
        when(service.setEstimatedDelivery("uuid-1", when)).thenReturn(dto("uuid-1"));

        var resp = controller.setEstimatedDelivery("uuid-1", new SetEstimatedDeliveryRequest(when));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().message()).isEqualTo("Estimated delivery date set");
        verify(service).setEstimatedDelivery("uuid-1", when);
    }

    @Test
    @DisplayName("markDelivered returns OK with delivered message")
    void markDelivered() {
        when(service.markDelivered("uuid-1")).thenReturn(dto("uuid-1"));

        var resp = controller.markDelivered("uuid-1");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().message()).isEqualTo("Shipment marked as delivered");
    }

    @Test
    @DisplayName("listByStatus forwards pagination params and returns the page")
    void listByStatus() {
        Page<ShipmentDTO> page = new PageImpl<>(List.of(dto("uuid-1")));
        when(service.listByStatus("IN_TRANSIT", 2, 25)).thenReturn(page);

        var resp = controller.listByStatus("IN_TRANSIT", 2, 25);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().data().getTotalElements()).isEqualTo(1);
        verify(service).listByStatus("IN_TRANSIT", 2, 25);
    }
}
