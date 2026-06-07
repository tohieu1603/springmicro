package com.hieu.auth_service.domain.models.refreshtoken.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TokenExpiry VO (unit)")
class TokenExpiryTest {

    @Test
    @DisplayName("a future expiry is valid with positive remaining seconds")
    void futureExpiry() {
        TokenExpiry expiry = TokenExpiry.fromDaysFromNow(7);

        assertThat(expiry.isExpired()).isFalse();
        assertThat(expiry.isValid()).isTrue();
        assertThat(expiry.getRemainingSeconds()).isPositive();
    }

    @Test
    @DisplayName("a past expiry is expired with zero remaining seconds")
    void pastExpiry() {
        TokenExpiry expiry = TokenExpiry.of(Instant.now().minus(1, ChronoUnit.HOURS));

        assertThat(expiry.isExpired()).isTrue();
        assertThat(expiry.isValid()).isFalse();
        assertThat(expiry.getRemainingSeconds()).isZero();
        assertThat(expiry.willExpireWithin(60)).isTrue();
    }

    @Test
    @DisplayName("willExpireWithin() compares against the remaining window")
    void willExpireWithin() {
        TokenExpiry expiry = TokenExpiry.fromHoursFromNow(1); // ~3600s out

        assertThat(expiry.willExpireWithin(10)).isFalse();
        assertThat(expiry.willExpireWithin(7200)).isTrue();
    }

    @Test
    @DisplayName("rejects non-positive durations and null instants")
    void rejectsInvalid() {
        assertThatThrownBy(() -> TokenExpiry.fromDaysFromNow(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TokenExpiry.fromHoursFromNow(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TokenExpiry.of(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
