package com.hieu.voucher_service.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVoucherRequest {

    @Pattern(regexp = "PERCENTAGE|FIXED_AMOUNT", message = "Type must be PERCENTAGE or FIXED_AMOUNT")
    private String type;

    @Positive(message = "Discount value must be positive")
    private BigDecimal discountValue;

    @Positive(message = "Min order amount must be positive")
    private BigDecimal minOrderAmount;

    @Positive(message = "Max discount amount must be positive")
    private BigDecimal maxDiscountAmount;

    @Positive(message = "Usage limit must be positive")
    private Integer usageLimit;

    private Instant startDate;
    private Instant endDate;
    private Boolean active;
    private String description;
}
