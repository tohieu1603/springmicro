package com.hieu.payment_service.integration.sepay;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;

/**
 * Generates SePay VietQR URLs for bank-transfer payments.
 */
@Service
public class SepayQrService {

    @Value("${sepay.qr-base-url:https://qr.sepay.vn/img}")
    private String qrBaseUrl;

    @Value("${sepay.bank-code:BIDV}")
    private String bankCode;

    @Value("${sepay.bank-account:96247CISI1}")
    private String bankAccount;

    @Value("${sepay.account-name:TO TRONG HIEU}")
    private String accountName;

    public String generateQrUrl(String orderId, BigDecimal amount) {
        return UriComponentsBuilder.fromUriString(qrBaseUrl)
                .queryParam("acc", bankAccount)
                .queryParam("bank", bankCode)
                .queryParam("amount", amount.toPlainString())
                .queryParam("des", orderId)
                .toUriString();
    }

    public String getBankCode()    { return bankCode; }
    public String getBankAccount() { return bankAccount; }
    public String getAccountName() { return accountName; }
}
