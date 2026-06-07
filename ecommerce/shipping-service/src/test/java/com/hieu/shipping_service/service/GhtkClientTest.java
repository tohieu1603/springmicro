package com.hieu.shipping_service.service;

import com.hieu.shipping_service.config.GhtkProperties;
import com.hieu.shipping_service.dto.CalculateFeeRequest;
import com.hieu.shipping_service.dto.CalculateFeeResponse;
import com.hieu.shipping_service.rest.client.GhtkFeignClient;
import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link GhtkClient}: the GHTK-live mapping path, the numeric
 * coercion of fee fields, and every branch into the local fallback estimator
 * (missing token, success=false, null fee, FeignException, generic Exception) plus the
 * intra-city vs long-haul fallback pricing. Feign client + props are mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GhtkClient (unit)")
class GhtkClientTest {

    @Mock GhtkFeignClient ghtk;

    private static GhtkProperties props(String token) {
        return new GhtkProperties(
                "https://services.ghtknet.com", token, "road",
                new GhtkProperties.Pick("Hà Nội", "Cầu Giấy", "Dịch Vọng", "1 Đường ABC"));
    }

    private static CalculateFeeRequest req(String province, int weightGrams, String transport) {
        return new CalculateFeeRequest(province, "District", "Ward", "Addr", weightGrams, 500_000L, transport);
    }

    @Test
    @DisplayName("missing token short-circuits to local fallback without calling GHTK")
    void missingToken_fallback() {
        var client = new GhtkClient(ghtk, props(""));

        var resp = client.calculateFee(req("Hà Nội", 1000, null));

        assertThat(resp.source()).isEqualTo("LOCAL_FALLBACK");
        verifyNoInteractions(ghtk);
    }

    @Test
    @DisplayName("null token short-circuits to local fallback")
    void nullToken_fallback() {
        var client = new GhtkClient(ghtk, props(null));
        var resp = client.calculateFee(req("Hà Nội", 1000, null));
        assertThat(resp.source()).isEqualTo("LOCAL_FALLBACK");
        verifyNoInteractions(ghtk);
    }

    @Test
    @DisplayName("successful GHTK response maps fee, insurance and delivery hours")
    void ghtkSuccess_mapsFee() {
        var client = new GhtkClient(ghtk, props("real-token"));
        when(ghtk.calculateFee(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyInt(), anyLong(), anyString(), anyString()))
                .thenReturn(Map.of("success", true,
                        "fee", Map.of("fee", 35_000, "insurance_fee", 2_000, "delivery", 48)));

        var resp = client.calculateFee(req("Hồ Chí Minh", 1500, null));

        assertThat(resp.source()).isEqualTo("GHTK_LIVE");
        assertThat(resp.fee()).isEqualTo(35_000L);
        assertThat(resp.insuranceFee()).isEqualTo(2_000L);
        assertThat(resp.deliveryTimeHours()).isEqualTo(48L);
    }

    @Test
    @DisplayName("string-typed numeric fields in the GHTK fee map are coerced to long")
    void ghtkSuccess_stringNumbersCoerced() {
        var client = new GhtkClient(ghtk, props("real-token"));
        when(ghtk.calculateFee(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyInt(), anyLong(), anyString(), anyString()))
                .thenReturn(Map.of("success", true,
                        "fee", Map.of("fee", "40000", "insurance_fee", "bad", "delivery", "24")));

        var resp = client.calculateFee(req("Hồ Chí Minh", 1000, null));

        assertThat(resp.fee()).isEqualTo(40_000L);
        assertThat(resp.insuranceFee()).isEqualTo(0L); // unparseable -> 0
        assertThat(resp.deliveryTimeHours()).isEqualTo(24L);
    }

    @Test
    @DisplayName("uses request transport when present, falls back to props.transport when null")
    void transportSelection() {
        var client = new GhtkClient(ghtk, props("real-token"));
        when(ghtk.calculateFee(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyInt(), anyLong(), anyString(), anyString()))
                .thenReturn(Map.of("success", true, "fee", Map.of("fee", 1, "insurance_fee", 0, "delivery", 1)));

        client.calculateFee(req("Hồ Chí Minh", 1000, "fly"));

        verify(ghtk).calculateFee(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyInt(), anyLong(),
                org.mockito.ArgumentMatchers.eq("fly"), anyString());
    }

    @Test
    @DisplayName("success=false envelope drops into fallback")
    void ghtkUnsuccess_fallback() {
        var client = new GhtkClient(ghtk, props("real-token"));
        when(ghtk.calculateFee(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyInt(), anyLong(), anyString(), anyString()))
                .thenReturn(Map.of("success", false, "message", "bad province"));

        var resp = client.calculateFee(req("Hà Nội", 1000, null));

        assertThat(resp.source()).isEqualTo("LOCAL_FALLBACK");
    }

    @Test
    @DisplayName("null response drops into fallback")
    void ghtkNullResponse_fallback() {
        var client = new GhtkClient(ghtk, props("real-token"));
        when(ghtk.calculateFee(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyInt(), anyLong(), anyString(), anyString()))
                .thenReturn(null);

        assertThat(client.calculateFee(req("Hà Nội", 1000, null)).source()).isEqualTo("LOCAL_FALLBACK");
    }

    @Test
    @DisplayName("success=true but missing fee map drops into fallback")
    void ghtkMissingFee_fallback() {
        var client = new GhtkClient(ghtk, props("real-token"));
        when(ghtk.calculateFee(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyInt(), anyLong(), anyString(), anyString()))
                .thenReturn(Map.of("success", true));

        assertThat(client.calculateFee(req("Hà Nội", 1000, null)).source()).isEqualTo("LOCAL_FALLBACK");
    }

    @Test
    @DisplayName("FeignException from GHTK drops into fallback")
    void feignException_fallback() {
        var client = new GhtkClient(ghtk, props("real-token"));
        when(ghtk.calculateFee(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyInt(), anyLong(), anyString(), anyString()))
                .thenThrow(mock(FeignException.class));

        assertThat(client.calculateFee(req("Hà Nội", 1000, null)).source()).isEqualTo("LOCAL_FALLBACK");
    }

    @Test
    @DisplayName("generic RuntimeException from GHTK drops into fallback")
    void genericException_fallback() {
        var client = new GhtkClient(ghtk, props("real-token"));
        when(ghtk.calculateFee(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyInt(), anyLong(), anyString(), anyString()))
                .thenThrow(new RuntimeException("boom"));

        assertThat(client.calculateFee(req("Hà Nội", 1000, null)).source()).isEqualTo("LOCAL_FALLBACK");
    }

    @Test
    @DisplayName("fallback: intra-city 1kg = base 22k, 24h, no surcharge")
    void fallback_intracityBase() {
        var client = new GhtkClient(ghtk, props(""));

        var resp = client.calculateFee(req("Hà Nội", 1000, null));

        assertThat(resp.fee()).isEqualTo(22_000L);
        assertThat(resp.insuranceFee()).isEqualTo(0L);
        assertThat(resp.deliveryTimeHours()).isEqualTo(24L);
    }

    @Test
    @DisplayName("fallback: intra-city province name normalisation ignores 'Thành phố' prefix")
    void fallback_provinceNormalisation() {
        var client = new GhtkClient(ghtk, props(""));
        // props pick province = "Hà Nội"; request "Thành phố Hà Nội" should normalise equal -> intra-city
        var resp = client.calculateFee(req("Thành phố Hà Nội", 1000, null));
        assertThat(resp.deliveryTimeHours()).isEqualTo(24L); // intra-city hours
    }

    @Test
    @DisplayName("fallback: long-haul adds 18k surcharge and 72h")
    void fallback_longHaul() {
        var client = new GhtkClient(ghtk, props(""));

        var resp = client.calculateFee(req("Hồ Chí Minh", 1000, null));

        assertThat(resp.fee()).isEqualTo(22_000L + 18_000L);
        assertThat(resp.deliveryTimeHours()).isEqualTo(72L);
    }

    @Test
    @DisplayName("fallback: extra weight adds 5k per kg over 1kg (ceil)")
    void fallback_extraWeight() {
        var client = new GhtkClient(ghtk, props(""));

        // 2500g -> ceil = 3kg -> (3-1)*5000 = 10000 extra over 22000 base, intra-city
        var resp = client.calculateFee(req("Hà Nội", 2500, null));

        assertThat(resp.fee()).isEqualTo(22_000L + 10_000L);
    }

    @Test
    @DisplayName("fallback: null province treated as long-haul")
    void fallback_nullProvince() {
        var client = new GhtkClient(ghtk, props(""));

        var resp = client.calculateFee(req(null, 1000, null));

        assertThat(resp.fee()).isEqualTo(22_000L + 18_000L);
        assertThat(resp.deliveryTimeHours()).isEqualTo(72L);
        verify(ghtk, never()).calculateFee(any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyInt(), anyLong(), any(), any());
    }
}
