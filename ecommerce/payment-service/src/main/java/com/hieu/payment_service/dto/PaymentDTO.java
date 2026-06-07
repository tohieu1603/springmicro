package com.hieu.payment_service.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class PaymentDTO {

    private String id;
    private String orderId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private String method;
    private String status;
    private String transactionId;
    private String gatewayResponse;
    private Instant paidAt;
    private BigDecimal refundAmount;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
    private String idempotencyKey;

    // SePay QR payment fields
    private String qrCodeUrl;
    private String bankCode;
    private String bankAccount;
    private String accountName;
    private String transferContent;

    // MoMo / e-wallet redirect
    private String payUrl;
}
