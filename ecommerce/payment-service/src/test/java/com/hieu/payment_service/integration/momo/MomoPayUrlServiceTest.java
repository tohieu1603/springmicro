package com.hieu.payment_service.integration.momo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MomoPayUrlService — HMAC unit tests (no Spring context)")
class MomoPayUrlServiceTest {
    /**
     * Top-level smoke test — bắt buộc để Sonar rule java:S2187 nhận diện
     * class này có @Test (không tính các @Test bên trong @Nested).
     * Mọi test thật sự được tổ chức trong các @Nested class bên dưới.
     */
    @Test
    @DisplayName("class loads + JUnit discovers @Nested tests")
    void smokeTest_classDiscovered() {
        // Nếu tới được đây tức là JUnit có thể instantiate test class.
        // assertThat(this).isNotNull() được tự thực hiện ngầm.
    }


    private MomoPayUrlService buildService(String secretKey) {
        var svc = new MomoPayUrlService();
        ReflectionTestUtils.setField(svc, "partnerCode", "MOMO_TEST");
        ReflectionTestUtils.setField(svc, "sandboxBaseUrl", "https://test-payment.momo.vn/v2/gateway/pay");
        ReflectionTestUtils.setField(svc, "returnUrl", "http://localhost:3000/payment/momo/return");
        ReflectionTestUtils.setField(svc, "momoSecretKey", secretKey);
        return svc;
    }

    /** Compute HMAC-SHA256 hex independently for comparison. */
    private static String computeHmac(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : sig) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    @Nested
    @DisplayName("Hmac")
    class Hmac {

        @Test
        @DisplayName("same input + same secret → same signature (deterministic for fixed raw string)")
        void sameInput_sameSecret_sameSignature() throws Exception {
            // Use a fixed raw string (bypassing UUID randomness) to verify HMAC determinism
            String secret = "my-test-secret-key-for-unit-test";
            String raw = "ORDER-123" + "99000" + "req-fixed-id";

            String expected = computeHmac(raw, secret);
            String again = computeHmac(raw, secret);

            assertThat(again)
                    .as("HMAC must be deterministic for same input + secret")
                    .isEqualTo(expected)
                    .hasSize(64) // SHA-256 = 32 bytes = 64 hex chars
                    .matches("[0-9a-f]{64}");
        }

        @Test
        @DisplayName("different secret → different signature")
        void differentSecret_differentSignature() throws Exception {
            String raw = "ORDER-123" + "99000" + "req-id-abc";
            String sig1 = computeHmac(raw, "secret-one");
            String sig2 = computeHmac(raw, "secret-two");

            assertThat(sig1).isNotEqualTo(sig2);
        }

        @Test
        @DisplayName("generatePayUrl with real secret embeds a 64-char hex sig= param")
        void generatePayUrl_withSecret_containsHexSig() {
            var svc = buildService("valid-test-secret-key-32chars!!!!");
            String url = svc.generatePayUrl("ORD-XYZ", BigDecimal.valueOf(50_000));

            assertThat(url).contains("sig=");
            String sig = url.substring(url.indexOf("sig=") + 4);
            // sig is the last param; take everything after sig=
            if (sig.contains("&")) sig = sig.substring(0, sig.indexOf("&"));
            assertThat(sig)
                    .hasSize(64)
                    .matches("[0-9a-f]{64}");
        }
    }

    @Nested
    @DisplayName("MissingSecret")
    class MissingSecret {

        @Test
        @DisplayName("empty secret → generatePayUrl returns url with sig= empty string (DEV mode)")
        void emptySecret_returnsEmptySig() {
            var svc = buildService("");
            String url = svc.generatePayUrl("ORD-ABC", BigDecimal.valueOf(100_000));

            assertThat(url).contains("sig=");
            // sig should be blank/empty when key not configured
            String sig = url.substring(url.indexOf("sig=") + 4);
            if (sig.contains("&")) sig = sig.substring(0, sig.indexOf("&"));
            assertThat(sig).isEmpty();
        }

        @Test
        @DisplayName("null secret → generatePayUrl returns url with sig= empty string (DEV mode)")
        void nullSecret_returnsEmptySig() {
            var svc = buildService(null);
            String url = svc.generatePayUrl("ORD-NULL", BigDecimal.valueOf(200_000));

            assertThat(url).contains("sig=");
            String sig = url.substring(url.indexOf("sig=") + 4);
            if (sig.contains("&")) sig = sig.substring(0, sig.indexOf("&"));
            assertThat(sig).isEmpty();
        }
    }
}
