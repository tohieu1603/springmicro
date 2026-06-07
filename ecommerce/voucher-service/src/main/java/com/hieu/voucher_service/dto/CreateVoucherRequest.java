package com.hieu.voucher_service.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
public class CreateVoucherRequest {

    @NotBlank(message = "Code is required")
    @Size(min = 3, max = 50, message = "Code must be between 3 and 50 characters")
    private String code;

    @NotNull(message = "Type is required")
    @Pattern(regexp = "PERCENTAGE|FIXED_AMOUNT", message = "Type must be PERCENTAGE or FIXED_AMOUNT")
    private String type;

    @NotNull(message = "Discount value is required")
    @Positive(message = "Discount value must be positive")
    private BigDecimal discountValue;

    @Positive(message = "Min order amount must be positive")
    private BigDecimal minOrderAmount;

    @Positive(message = "Max discount amount must be positive")
    private BigDecimal maxDiscountAmount;

    @Positive(message = "Usage limit must be positive")
    private Integer usageLimit;

    @Positive(message = "Usage limit per user must be positive")
    private Integer usageLimitPerUser;

    private Instant startDate;

    @Future(message = "endDate must be in the future")
    private Instant endDate;

    private String description;
}
