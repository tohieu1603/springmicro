package com.hieu.voucher_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyVoucherResponse {

    private String code;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String message;
}
