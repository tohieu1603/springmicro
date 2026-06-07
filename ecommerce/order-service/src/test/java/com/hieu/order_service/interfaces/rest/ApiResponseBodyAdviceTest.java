package com.hieu.order_service.interfaces.rest;

import com.hieu.common.api.ApiResponse;
import com.hieu.common.error.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** Pure unit tests for the response envelope advice. */
class ApiResponseBodyAdviceTest {

    private final ApiResponseBodyAdvice advice = new ApiResponseBodyAdvice();

    private Object write(Object body) {
        return advice.beforeBodyWrite(body, null, MediaType.APPLICATION_JSON, null, null, null);
    }

    @Test
    @DisplayName("supports returns true for any return type")
    void supports() {
        assertThat(advice.supports(null, null)).isTrue();
    }

    @Test
    @DisplayName("plain DTO body is wrapped in a successful ApiResponse")
    void wrapsPlainBody() {
        var dto = new Object();

        var result = write(dto);

        assertThat(result).isInstanceOf(ApiResponse.class);
        var resp = (ApiResponse<?>) result;
        assertThat(resp.success()).isTrue();
        assertThat(resp.code()).isEqualTo("OK");
        assertThat(resp.data()).isSameAs(dto);
    }

    @Test
    @DisplayName("existing ApiResponse passes through untouched")
    void passesThroughApiResponse() {
        var existing = ApiResponse.ok("already wrapped");

        assertThat(write(existing)).isSameAs(existing);
    }

    @Test
    @DisplayName("ErrorResponse passes through untouched")
    void passesThroughErrorResponse() {
        var err = new ErrorResponse("CODE", "msg", "/p", Instant.now(), null, null, null);

        assertThat(write(err)).isSameAs(err);
    }

    @Test
    @DisplayName("String body passes through to avoid corrupting the StringHttpMessageConverter contract")
    void passesThroughString() {
        var s = "swagger-ui";

        assertThat(write(s)).isSameAs(s);
    }

    @Test
    @DisplayName("null body is wrapped (data is null but envelope is success)")
    void wrapsNull() {
        var result = write(null);

        assertThat(result).isInstanceOf(ApiResponse.class);
        assertThat(((ApiResponse<?>) result).data()).isNull();
    }
}
