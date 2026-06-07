package com.hieu.voucher_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateVoucherRequest {

    @NotBlank(message = "Code is required")
    private String code;

    @NotNull(message = "Order amount is required")
    @Positive(message = "Order amount must be positive")
    private BigDecimal orderAmount;

    /** userId (UUID string) — enforce per-user limit và targetUserIds check. */
    @NotBlank(message = "userId is required")
    private String userId;

    /** orderId để tạo idempotent usage record, dùng khi release. */
    @NotBlank(message = "orderId is required")
    private String orderId;

    /** productIds trong order — cần khi voucher có applicableProductIds restriction. */
    private List<String> productIds;
}
