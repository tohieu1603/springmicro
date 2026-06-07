package com.hieu.voucher_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseVoucherRequest {

    @NotBlank(message = "Code is required")
    private String code;

    /** orderId để idempotent release — nếu không có record thì bỏ qua. */
    @NotBlank(message = "orderId is required")
    private String orderId;
}
