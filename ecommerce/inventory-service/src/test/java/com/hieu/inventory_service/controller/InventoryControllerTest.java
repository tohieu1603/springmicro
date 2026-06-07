package com.hieu.inventory_service.controller;

import com.hieu.inventory_service.dto.CreateInventoryRequest;
import com.hieu.inventory_service.dto.InventoryDTO;
import com.hieu.inventory_service.dto.PageDTO;
import com.hieu.inventory_service.dto.ReservationRequest;
import com.hieu.inventory_service.dto.ReservationResult;
import com.hieu.inventory_service.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for the branching in {@link InventoryController}: HTTP status mapping,
 * the {@code Map.of("success", ...)} envelope on confirm/release, and the request-body /
 * principal parsing logic inside {@code adjustStock} (Number vs String delta, principal ->
 * actor fallback). {@link InventoryService} is mocked; we assert the returned
 * {@link ResponseEntity} and the exact delegate arguments. No MockMvc / Spring context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryController (unit)")
class InventoryControllerTest {

    @Mock InventoryService inventoryService;
    @Captor ArgumentCaptor<Integer> deltaCaptor;
    @Captor ArgumentCaptor<String> actorCaptor;
    @Captor ArgumentCaptor<String> noteCaptor;

    InventoryController controller;

    @BeforeEach
    void setup() {
        controller = new InventoryController(inventoryService);
    }

    private static InventoryDTO dto(String productId) {
        return InventoryDTO.builder().id("1").productId(productId).sku("SKU-" + productId)
                .quantity(10).reservedQuantity(0).availableQuantity(10).build();
    }

    @Test
    @DisplayName("create returns 201 CREATED with the service DTO")
    void create_returns201() {
        var req = new CreateInventoryRequest("50", "SKU-50", 100, 10);
        when(inventoryService.create("50", "SKU-50", 100, 10)).thenReturn(dto("50"));

        ResponseEntity<InventoryDTO> resp = controller.create(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().getProductId()).isEqualTo("50");
    }

    @Test
    @DisplayName("getByProductId returns 200 OK")
    void getByProductId_returns200() {
        when(inventoryService.getByProductId("7")).thenReturn(dto("7"));
        ResponseEntity<InventoryDTO> resp = controller.getByProductId("7");
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getProductId()).isEqualTo("7");
    }

    @Test
    @DisplayName("getBySku returns 200 OK")
    void getBySku_returns200() {
        when(inventoryService.getBySku("SKU-7")).thenReturn(dto("7"));
        ResponseEntity<InventoryDTO> resp = controller.getBySku("SKU-7");
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getSku()).isEqualTo("SKU-7");
    }

    @Test
    @DisplayName("getAll forwards page/size and returns the PageDTO")
    void getAll_returnsPage() {
        var pageDto = new PageDTO<>(List.of(dto("1")), 2, 25, 51L, 3);
        when(inventoryService.getAll(2, 25)).thenReturn(pageDto);

        ResponseEntity<PageDTO<InventoryDTO>> resp = controller.getAll(2, 25);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().page()).isEqualTo(2);
        assertThat(resp.getBody().totalElements()).isEqualTo(51L);
    }

    @Test
    @DisplayName("reserve returns 200 OK with the ReservationResult")
    void reserve_returns200() {
        var req = new ReservationRequest("O-1", List.of(new ReservationRequest.ReservationItem("1", 2)));
        when(inventoryService.reserveStock(req)).thenReturn(ReservationResult.success("O-1"));

        ResponseEntity<ReservationResult> resp = controller.reserve(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().success()).isTrue();
    }

    @Test
    @DisplayName("confirm wraps the result in {success=true}")
    void confirm_wrapsSuccessTrue() {
        when(inventoryService.confirmReservation("O-1")).thenReturn(ReservationResult.success("O-1"));
        ResponseEntity<Map<String, Boolean>> resp = controller.confirm("O-1");
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("success", true);
    }

    @Test
    @DisplayName("release wraps a failed result in {success=false}")
    void release_wrapsSuccessFalse() {
        when(inventoryService.releaseReservation("O-1")).thenReturn(ReservationResult.failure("nope"));
        ResponseEntity<Map<String, Boolean>> resp = controller.release("O-1");
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("success", false);
    }

    // ── adjustStock parsing branches ───────────────────────────────────────────

    @Test
    @DisplayName("adjustStock: numeric delta + String note + principal -> actor=principal.toString()")
    void adjustStock_numericDelta_principalActor() {
        when(inventoryService.adjustStock(eq("7"), deltaCaptor.capture(),
                actorCaptor.capture(), noteCaptor.capture())).thenReturn(dto("7"));
        Map<String, Object> body = new HashMap<>();
        body.put("delta", 25);            // Number branch
        body.put("note", "restock");      // String branch

        ResponseEntity<InventoryDTO> resp = controller.adjustStock("7", body, "user-42");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deltaCaptor.getValue()).isEqualTo(25);
        assertThat(noteCaptor.getValue()).isEqualTo("restock");
        assertThat(actorCaptor.getValue()).isEqualTo("user-42");
    }

    @Test
    @DisplayName("adjustStock: String delta is parsed; null principal -> actor defaults to ADMIN")
    void adjustStock_stringDelta_defaultActor() {
        when(inventoryService.adjustStock(eq("7"), deltaCaptor.capture(),
                actorCaptor.capture(), noteCaptor.capture())).thenReturn(dto("7"));
        Map<String, Object> body = new HashMap<>();
        body.put("delta", "-15");         // String -> Integer.parseInt branch

        ResponseEntity<InventoryDTO> resp = controller.adjustStock("7", body, null);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deltaCaptor.getValue()).isEqualTo(-15);
        assertThat(noteCaptor.getValue()).isNull();      // non-String note -> null
        assertThat(actorCaptor.getValue()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("adjustStock: non-String note (e.g. number) is dropped to null")
    void adjustStock_nonStringNoteDropped() {
        when(inventoryService.adjustStock(eq("7"), deltaCaptor.capture(),
                actorCaptor.capture(), noteCaptor.capture())).thenReturn(dto("7"));
        Map<String, Object> body = new HashMap<>();
        body.put("delta", 5);
        body.put("note", 999);            // not a String

        controller.adjustStock("7", body, "actorX");

        assertThat(noteCaptor.getValue()).isNull();
        assertThat(actorCaptor.getValue()).isEqualTo("actorX");
    }
}
