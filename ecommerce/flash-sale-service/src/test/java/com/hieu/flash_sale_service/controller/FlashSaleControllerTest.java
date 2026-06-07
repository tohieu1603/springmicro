package com.hieu.flash_sale_service.controller;

import com.hieu.common.api.ApiResponse;
import com.hieu.common.security.AuthenticatedUser;
import com.hieu.flash_sale_service.dto.AvailabilityResponse;
import com.hieu.flash_sale_service.dto.CreateFlashSaleRequest;
import com.hieu.flash_sale_service.dto.FlashSaleDTO;
import com.hieu.flash_sale_service.dto.PageDTO;
import com.hieu.flash_sale_service.dto.ParticipateRequest;
import com.hieu.flash_sale_service.dto.ParticipateResponse;
import com.hieu.flash_sale_service.entity.FlashSaleStatus;
import com.hieu.flash_sale_service.service.FlashSaleApplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link FlashSaleController}. The application service is mocked and the
 * controller is exercised directly (no MockMvc / Spring context). Asserts the HTTP status,
 * envelope wrapping, optional success message and — for participate — that the authenticated
 * principal's userId is forwarded to the service.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FlashSaleController (unit)")
class FlashSaleControllerTest {

    @Mock FlashSaleApplicationService service;
    @InjectMocks FlashSaleController controller;

    private static FlashSaleDTO dto(String id) {
        return new FlashSaleDTO(id, "p1", "Product One",
                BigDecimal.valueOf(100), BigDecimal.valueOf(80),
                10, 0, 2, Instant.now(), Instant.now().plusSeconds(3600),
                FlashSaleStatus.SCHEDULED, "desc", null, null, 0L);
    }

    private static PageDTO<FlashSaleDTO> page() {
        return new PageDTO<>(List.of(dto("1")), 0, 20, 1, 1, true, true);
    }

    @Test
    @DisplayName("createSale -> 201 CREATED with payload + 'Flash sale created' message")
    void createSale() {
        var request = new CreateFlashSaleRequest("p1", "Product One",
                BigDecimal.valueOf(100), BigDecimal.valueOf(80), 10, 2,
                Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200), "desc");
        var created = dto("42");
        when(service.createSale(request)).thenReturn(created);

        ResponseEntity<ApiResponse<FlashSaleDTO>> resp = controller.createSale(request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().success()).isTrue();
        assertThat(resp.getBody().data()).isSameAs(created);
        assertThat(resp.getBody().message()).isEqualTo("Flash sale created");
    }

    @Test
    @DisplayName("listAll -> 200 OK forwarding default pagination args to the service")
    void listAll() {
        when(service.listAllSales(0, 20)).thenReturn(page());

        ResponseEntity<ApiResponse<PageDTO<FlashSaleDTO>>> resp = controller.listAll(0, 20);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().data().content()).hasSize(1);
        verify(service).listAllSales(0, 20);
    }

    @Test
    @DisplayName("activate -> 200 OK with 'Flash sale activated' message")
    void activate() {
        when(service.activateSale("1")).thenReturn(dto("1"));

        ResponseEntity<ApiResponse<FlashSaleDTO>> resp = controller.activate("1");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().message()).isEqualTo("Flash sale activated");
        verify(service).activateSale("1");
    }

    @Test
    @DisplayName("end -> 200 OK with 'Flash sale ended' message")
    void end() {
        when(service.endSale("1")).thenReturn(dto("1"));

        ResponseEntity<ApiResponse<FlashSaleDTO>> resp = controller.end("1");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().message()).isEqualTo("Flash sale ended");
    }

    @Test
    @DisplayName("cancel -> 200 OK with 'Flash sale cancelled' message")
    void cancel() {
        when(service.cancelSale("1")).thenReturn(dto("1"));

        ResponseEntity<ApiResponse<FlashSaleDTO>> resp = controller.cancel("1");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().message()).isEqualTo("Flash sale cancelled");
    }

    @Test
    @DisplayName("listActive -> 200 OK forwarding pagination to listActiveSales")
    void listActive() {
        when(service.listActiveSales(2, 5)).thenReturn(page());

        ResponseEntity<ApiResponse<PageDTO<FlashSaleDTO>>> resp = controller.listActive(2, 5);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service).listActiveSales(2, 5);
    }

    @Test
    @DisplayName("getById -> 200 OK with the sale payload (no message)")
    void getById() {
        var found = dto("9");
        when(service.getSale("9")).thenReturn(found);

        ResponseEntity<ApiResponse<FlashSaleDTO>> resp = controller.getById("9");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().data()).isSameAs(found);
        assertThat(resp.getBody().message()).isNull();
    }

    @Test
    @DisplayName("availability -> 200 OK with the availability payload")
    void availability() {
        var avail = new AvailabilityResponse(10, 3, 7, true, null);
        when(service.checkAvailability("4")).thenReturn(avail);

        ResponseEntity<ApiResponse<AvailabilityResponse>> resp = controller.availability("4");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().data()).isSameAs(avail);
    }

    @Test
    @DisplayName("participate -> 200 OK and forwards the principal's userId + requested quantity")
    void participate() {
        var principal = new AuthenticatedUser("user-123", "alice",
                List.of("ROLE_USER"), List.of());
        var request = new ParticipateRequest(3);
        var response = new ParticipateResponse(true, "100", 7, null);
        when(service.participate("5", "user-123", 3)).thenReturn(response);

        ResponseEntity<ApiResponse<ParticipateResponse>> resp =
                controller.participate("5", request, principal);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().data()).isSameAs(response);
        verify(service).participate("5", "user-123", 3);
    }
}
