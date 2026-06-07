package com.hieu.flash_sale_service.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FlashSaleStatus state machine (unit)")
class FlashSaleStatusTest {

    @Test
    @DisplayName("SCHEDULED may transition only to ACTIVE or CANCELLED")
    void scheduledTransitions() {
        assertThat(FlashSaleStatus.SCHEDULED.canTransitionTo(FlashSaleStatus.ACTIVE)).isTrue();
        assertThat(FlashSaleStatus.SCHEDULED.canTransitionTo(FlashSaleStatus.CANCELLED)).isTrue();
        assertThat(FlashSaleStatus.SCHEDULED.canTransitionTo(FlashSaleStatus.ENDED)).isFalse();
        assertThat(FlashSaleStatus.SCHEDULED.canTransitionTo(FlashSaleStatus.SCHEDULED)).isFalse();
    }

    @Test
    @DisplayName("ACTIVE may transition only to ENDED or CANCELLED")
    void activeTransitions() {
        assertThat(FlashSaleStatus.ACTIVE.canTransitionTo(FlashSaleStatus.ENDED)).isTrue();
        assertThat(FlashSaleStatus.ACTIVE.canTransitionTo(FlashSaleStatus.CANCELLED)).isTrue();
        assertThat(FlashSaleStatus.ACTIVE.canTransitionTo(FlashSaleStatus.SCHEDULED)).isFalse();
        assertThat(FlashSaleStatus.ACTIVE.canTransitionTo(FlashSaleStatus.ACTIVE)).isFalse();
    }

    @Test
    @DisplayName("ENDED and CANCELLED are terminal")
    void terminalStates() {
        for (FlashSaleStatus target : FlashSaleStatus.values()) {
            assertThat(FlashSaleStatus.ENDED.canTransitionTo(target)).isFalse();
            assertThat(FlashSaleStatus.CANCELLED.canTransitionTo(target)).isFalse();
        }
    }
}
