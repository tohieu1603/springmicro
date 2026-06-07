package com.hieu.payment_service.integration.momo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Generates MoMo sandbox redirect URLs for e-wallet payments.
 */
@Service
@Slf4j
public class MomoPayUrlService {

    @Value("${momo.partner-code:MOMO_DEMO}")
    private String partnerCode;

    @Value("${momo.sandbox-base-url:https://test-payment.momo.vn/v2/gateway/pay}")
    private String sandboxBaseUrl;

    @Value("${momo.return-url:http://localhost:3000/payment/momo/return}")
    private String returnUrl;

    @Value("${payment.momo.secret-key:}")
    private String momoSecretKey;

    public String generatePayUrl(String orderId, BigDecimal amount) {
        String requestId = UUID.randomUUID().toString();
        String raw = orderId + amount + requestId;
        String sig = hmacSha256(raw);
        return sandboxBaseUrl
                + "?partnerCode=" + partnerCode
                + "&orderId=" + orderId
                + "&requestId=" + requestId
                + "&amount=" + amount
                + "&returnUrl=" + URLEncoder.encode(returnUrl, StandardCharsets.UTF_8)
                + "&sig=" + sig;
    }

    /** Real HMAC-SHA256 signature; falls back to empty string in DEV when key is not configured. */
    private String hmacSha256(String data) {
        if (momoSecretKey == null || momoSecretKey.isBlank()) {
            log.warn("momo.secret-key not configured — signature is unsigned (DEV ONLY)");
            return "";
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(momoSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : sig) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC SHA-256 failed", e);
        }
    }

    public String getPartnerCode() { return partnerCode; }
}
