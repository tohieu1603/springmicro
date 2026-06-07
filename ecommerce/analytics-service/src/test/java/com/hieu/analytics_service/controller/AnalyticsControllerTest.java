package com.hieu.analytics_service.controller;

import com.hieu.analytics_service.service.AnalyticsQueryService;
import com.hieu.common.api.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit test of {@link AnalyticsController}: mocks {@link AnalyticsQueryService}
 * and asserts the ResponseEntity status + body wrapping (no MockMvc, no Spring context).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsController — Unit")
class AnalyticsControllerTest {

    @Mock
    private AnalyticsQueryService queryService;

    @InjectMocks
    private AnalyticsController controller;

    private final Instant from = Instant.parse("2025-01-01T00:00:00Z");
    private final Instant to = Instant.parse("2025-01-31T00:00:00Z");

    @Test
    @DisplayName("summary wraps service map in 200 OK ApiResponse with the same payload")
    void summary_wrapsInApiResponseOk() {
        Map<String, Object> svcResult = Map.of("totalEvents", 42L, "orderPlaced", 7L);
        when(queryService.summary(from, to)).thenReturn(svcResult);

        ResponseEntity<ApiResponse<Map<String, Object>>> resp = controller.summary(from, to);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<Map<String, Object>> body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.success()).isTrue();
        assertThat(body.code()).isEqualTo("OK");
        assertThat(body.data()).isSameAs(svcResult);
        verify(queryService).summary(from, to);
    }

    @Test
    @DisplayName("logs forwards all four params verbatim and returns the raw service map at 200 OK")
    void logs_forwardsParamsAndReturnsRawMap() {
        Map<String, Object> svcResult = Map.of("content", List.of(), "totalElements", 0L);
        when(queryService.searchLogs("err", "ERROR", "order-service", 50)).thenReturn(svcResult);

        ResponseEntity<Map<String, Object>> resp =
                controller.logs("err", "ERROR", "order-service", 50);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isSameAs(svcResult);
        verify(queryService).searchLogs("err", "ERROR", "order-service", 50);
    }

    @Test
    @DisplayName("logs passes null optional filters through unchanged")
    void logs_passesNullFiltersThrough() {
        Map<String, Object> svcResult = Map.of("content", List.of());
        when(queryService.searchLogs(null, null, null, 100)).thenReturn(svcResult);

        ResponseEntity<Map<String, Object>> resp = controller.logs(null, null, null, 100);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isSameAs(svcResult);
        verify(queryService).searchLogs(null, null, null, 100);
    }

    @Test
    @DisplayName("revenue returns the daily series list at 200 OK without re-wrapping")
    void revenue_returnsSeriesList() {
        List<Map<String, Object>> series = List.of(
                Map.of("date", "2025-01-01", "revenue", 10d),
                Map.of("date", "2025-01-02", "revenue", 0d));
        when(queryService.revenueByDay(from, to)).thenReturn(series);

        ResponseEntity<List<Map<String, Object>>> resp = controller.revenue(from, to);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isSameAs(series);
        verify(queryService).revenueByDay(from, to);
    }
}
