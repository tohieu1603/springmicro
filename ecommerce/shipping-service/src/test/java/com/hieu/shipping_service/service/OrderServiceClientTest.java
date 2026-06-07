package com.hieu.shipping_service.service;

import com.hieu.common.api.ApiResponse;
import com.hieu.shipping_service.rest.client.OrderClient;
import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link OrderServiceClient}: the degrade-to-empty contract. The
 * Feign {@link OrderClient} is mocked; we assert that present data is unwrapped, while
 * null response / null data / FeignException all yield {@link java.util.Optional#empty()}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceClient (unit)")
class OrderServiceClientTest {

    @Mock OrderClient orderClient;
    @InjectMocks OrderServiceClient service;

    @Test
    @DisplayName("returns the order data map when present")
    void fetchOrder_present() {
        var data = Map.<String, Object>of("orderId", "ORD-1", "city", "HCM");
        when(orderClient.getOrder("ORD-1")).thenReturn(ApiResponse.ok(data));

        var result = service.fetchOrder("ORD-1");

        assertThat(result).isPresent();
        assertThat(result.get()).containsEntry("city", "HCM");
    }

    @Test
    @DisplayName("empty when the Feign response is null")
    void fetchOrder_nullResponse() {
        when(orderClient.getOrder("ORD-1")).thenReturn(null);
        assertThat(service.fetchOrder("ORD-1")).isEmpty();
    }

    @Test
    @DisplayName("empty when the response carries null data")
    void fetchOrder_nullData() {
        when(orderClient.getOrder("ORD-1")).thenReturn(ApiResponse.ok(null));
        assertThat(service.fetchOrder("ORD-1")).isEmpty();
    }

    @Test
    @DisplayName("empty when order-service throws FeignException")
    void fetchOrder_feignException() {
        when(orderClient.getOrder("ORD-1")).thenThrow(mock(FeignException.class));
        assertThat(service.fetchOrder("ORD-1")).isEmpty();
    }
}
