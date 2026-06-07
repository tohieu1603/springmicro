package com.hieu.voucher_service.dto;

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
public class VoucherDTO {

    private String id;
    private String code;
    private String type;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private Integer usageLimit;
    private int usedCount;
    private Instant startDate;
    private Instant endDate;
    private boolean active;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
