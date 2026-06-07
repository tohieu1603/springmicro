package com.hieu.voucher_service.controller;

import com.hieu.common.api.ApiResponse;
import com.hieu.common.security.AuthenticatedUser;
import com.hieu.voucher_service.dto.ApplyVoucherResponse;
import com.hieu.voucher_service.dto.CreateVoucherRequest;
import com.hieu.voucher_service.dto.ReleaseVoucherRequest;
import com.hieu.voucher_service.dto.UpdateVoucherRequest;
import com.hieu.voucher_service.dto.ValidateVoucherRequest;
import com.hieu.voucher_service.dto.VoucherDTO;
import com.hieu.voucher_service.service.VoucherApplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link VoucherController}. The application service is mocked;
 * we assert the returned {@link ResponseEntity} status code and the {@link ApiResponse}
 * envelope (success flag / message / payload) plus the values forwarded to the service.
 * No MockMvc, no Spring context.
 */
@ExtendWith(MockitoExtension.class)
class VoucherControllerTest {

    @Mock
    private VoucherApplicationService voucherService;

    @InjectMocks
    private VoucherController controller;

    @Test
    @DisplayName("createVoucher returns 201 CREATED with payload and 'Voucher created' message")
    void createVoucher() {
        CreateVoucherRequest req = new CreateVoucherRequest();
        VoucherDTO dto = VoucherDTO.builder().id("1").code("SAVE10").build();
        when(voucherService.createVoucher(req)).thenReturn(dto);

        ResponseEntity<ApiResponse<VoucherDTO>> resp = controller.createVoucher(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().success()).isTrue();
        assertThat(resp.getBody().message()).isEqualTo("Voucher created");
        assertThat(resp.getBody().data()).isSameAs(dto);
    }

    @Test
    @DisplayName("listVouchers returns 200 OK and forwards page/size to service")
    void listVouchers() {
        Page<VoucherDTO> page = new PageImpl<>(List.of(VoucherDTO.builder().id("1").build()));
        when(voucherService.listVouchers(2, 25)).thenReturn(page);

        ResponseEntity<ApiResponse<Page<VoucherDTO>>> resp = controller.listVouchers(2, 25);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().success()).isTrue();
        assertThat(resp.getBody().data()).isSameAs(page);
        verify(voucherService).listVouchers(2, 25);
    }

    @Test
    @DisplayName("listActiveVouchers returns 200 OK and forwards page/size to service")
    void listActiveVouchers() {
        Page<VoucherDTO> page = new PageImpl<>(List.of(VoucherDTO.builder().id("7").build()));
        when(voucherService.listActiveVouchers(0, 10)).thenReturn(page);

        ResponseEntity<ApiResponse<Page<VoucherDTO>>> resp = controller.listActiveVouchers(0, 10);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().data()).isSameAs(page);
        verify(voucherService).listActiveVouchers(0, 10);
    }

    @Test
    @DisplayName("getVoucher returns 200 OK with the service DTO")
    void getVoucher() {
        VoucherDTO dto = VoucherDTO.builder().id("42").code("X").build();
        when(voucherService.getVoucher("42")).thenReturn(dto);

        ResponseEntity<ApiResponse<VoucherDTO>> resp = controller.getVoucher("42");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().success()).isTrue();
        assertThat(resp.getBody().data()).isSameAs(dto);
    }

    @Test
    @DisplayName("getVoucherByCode returns 200 OK with the service DTO")
    void getVoucherByCode() {
        VoucherDTO dto = VoucherDTO.builder().id("5").code("SUMMER").build();
        when(voucherService.getVoucherByCode("SUMMER")).thenReturn(dto);

        ResponseEntity<ApiResponse<VoucherDTO>> resp = controller.getVoucherByCode("SUMMER");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().data()).isSameAs(dto);
    }

    @Test
    @DisplayName("updateVoucher returns 200 OK with 'Voucher updated' message")
    void updateVoucher() {
        UpdateVoucherRequest req = new UpdateVoucherRequest();
        VoucherDTO dto = VoucherDTO.builder().id("9").build();
        when(voucherService.updateVoucher("9", req)).thenReturn(dto);

        ResponseEntity<ApiResponse<VoucherDTO>> resp = controller.updateVoucher("9", req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().message()).isEqualTo("Voucher updated");
        assertThat(resp.getBody().data()).isSameAs(dto);
    }

    @Test
    @DisplayName("deactivateVoucher returns 200 OK with 'Voucher deactivated' message")
    void deactivateVoucher() {
        VoucherDTO dto = VoucherDTO.builder().id("3").active(false).build();
        when(voucherService.deactivateVoucher("3")).thenReturn(dto);

        ResponseEntity<ApiResponse<VoucherDTO>> resp = controller.deactivateVoucher("3");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().message()).isEqualTo("Voucher deactivated");
        assertThat(resp.getBody().data()).isSameAs(dto);
    }

    @Test
    @DisplayName("validateAndApply unpacks the request fields into the service call and returns 200 OK")
    void validateAndApply() {
        ValidateVoucherRequest req = ValidateVoucherRequest.builder()
                .code("SAVE10")
                .orderAmount(BigDecimal.valueOf(200))
                .userId("user-1")
                .orderId("order-1")
                .productIds(List.of("p1", "p2"))
                .build();
        ApplyVoucherResponse applied = ApplyVoucherResponse.builder()
                .code("SAVE10")
                .discountAmount(BigDecimal.TEN)
                .finalAmount(BigDecimal.valueOf(190))
                .message("Voucher applied successfully")
                .build();
        when(voucherService.validateAndApply("SAVE10", BigDecimal.valueOf(200),
                "user-1", "order-1", List.of("p1", "p2"))).thenReturn(applied);

        AuthenticatedUser currentUser = new AuthenticatedUser("user-1", "testuser", List.of(), List.of());
        ResponseEntity<ApiResponse<ApplyVoucherResponse>> resp = controller.validateAndApply(req, currentUser);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().success()).isTrue();
        assertThat(resp.getBody().data()).isSameAs(applied);
        verify(voucherService).validateAndApply("SAVE10", BigDecimal.valueOf(200),
                "user-1", "order-1", List.of("p1", "p2"));
    }

    @Test
    @DisplayName("releaseVoucher delegates code/orderId and returns 200 OK with null data + message")
    void releaseVoucher() {
        ReleaseVoucherRequest req = ReleaseVoucherRequest.builder()
                .code("SAVE10")
                .orderId("order-99")
                .build();

        ResponseEntity<ApiResponse<Void>> resp = controller.releaseVoucher(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().success()).isTrue();
        assertThat(resp.getBody().message()).isEqualTo("Voucher released");
        assertThat(resp.getBody().data()).isNull();

        ArgumentCaptor<String> codeCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> orderCap = ArgumentCaptor.forClass(String.class);
        verify(voucherService).releaseVoucher(codeCap.capture(), orderCap.capture());
        assertThat(codeCap.getValue()).isEqualTo("SAVE10");
        assertThat(orderCap.getValue()).isEqualTo("order-99");
    }
}
