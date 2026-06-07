package com.hieu.payment_service.integration.sepay;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test for {@link SepayQrService} URL composition. @Value fields are set via
 * reflection (no Spring context) to verify the query parameters are wired correctly.
 */
@DisplayName("SepayQrService (unit)")
class SepayQrServiceTest {

    private SepayQrService service;

    @BeforeEach
    void setup() {
        service = new SepayQrService();
        ReflectionTestUtils.setField(service, "qrBaseUrl", "https://qr.sepay.vn/img");
        ReflectionTestUtils.setField(service, "bankCode", "BIDV");
        ReflectionTestUtils.setField(service, "bankAccount", "ACC123");
        ReflectionTestUtils.setField(service, "accountName", "TO TRONG HIEU");
    }

    @Test
    @DisplayName("builds a VietQR url with account, bank, amount and order description")
    void generateQrUrl() {
        String url = service.generateQrUrl("ORD-20260101-000001", new BigDecimal("150000.00"));

        assertThat(url).startsWith("https://qr.sepay.vn/img");
        assertThat(url).contains("acc=ACC123");
        assertThat(url).contains("bank=BIDV");
        assertThat(url).contains("amount=150000.00");
        assertThat(url).contains("des=ORD-20260101-000001");
    }

    @Test
    @DisplayName("exposes configured bank metadata")
    void getters() {
        assertThat(service.getBankCode()).isEqualTo("BIDV");
        assertThat(service.getBankAccount()).isEqualTo("ACC123");
        assertThat(service.getAccountName()).isEqualTo("TO TRONG HIEU");
    }
}
