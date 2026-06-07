package com.hieu.payment_service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class RefundRequest {

    private String reason;

    /** Optional — null means full refund. */
    private BigDecimal refundAmount;
}
