package com.hieu.shipping_service.domain;

import com.hieu.shipping_service.entity.ShipmentStatus;
import com.hieu.shipping_service.exception.InvalidShipmentStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ShipmentStatus — pure domain unit tests")
class ShipmentEntityTest {
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


    @Nested
    @DisplayName("StateMachine")
    class StateMachine {

        @Test
        @DisplayName("PENDING → PICKED_UP is valid")
        void pending_toPickedUp_valid() {
            assertThat(ShipmentStatus.PENDING.canTransitionTo(ShipmentStatus.PICKED_UP)).isTrue();
        }

        @Test
        @DisplayName("PICKED_UP → IN_TRANSIT is valid")
        void pickedUp_toInTransit_valid() {
            assertThat(ShipmentStatus.PICKED_UP.canTransitionTo(ShipmentStatus.IN_TRANSIT)).isTrue();
        }

        @Test
        @DisplayName("IN_TRANSIT → OUT_FOR_DELIVERY is valid")
        void inTransit_toOutForDelivery_valid() {
            assertThat(ShipmentStatus.IN_TRANSIT.canTransitionTo(ShipmentStatus.OUT_FOR_DELIVERY)).isTrue();
        }

        @Test
        @DisplayName("OUT_FOR_DELIVERY → DELIVERED is valid")
        void outForDelivery_toDelivered_valid() {
            assertThat(ShipmentStatus.OUT_FOR_DELIVERY.canTransitionTo(ShipmentStatus.DELIVERED)).isTrue();
        }

        @Test
        @DisplayName("DELIVERED → PICKED_UP (backwards) throws via service guard")
        void delivered_toPreviousState_throws() {
            // The service uses canTransitionTo; verify it returns false for backwards move
            boolean allowed = ShipmentStatus.DELIVERED.canTransitionTo(ShipmentStatus.PICKED_UP);
            assertThat(allowed).isFalse();

            // Simulate service guard
            assertThatThrownBy(() -> {
                if (!ShipmentStatus.DELIVERED.canTransitionTo(ShipmentStatus.PICKED_UP)) {
                    throw new InvalidShipmentStateException(
                            "Cannot transition from DELIVERED to PICKED_UP");
                }
            }).isInstanceOf(InvalidShipmentStateException.class);
        }

        @Test
        @DisplayName("cancel (RETURNED) only from PENDING")
        void cancel_onlyFromPending() {
            assertThat(ShipmentStatus.PENDING.canTransitionTo(ShipmentStatus.RETURNED)).isTrue();
            assertThat(ShipmentStatus.IN_TRANSIT.canTransitionTo(ShipmentStatus.RETURNED)).isFalse();
            assertThat(ShipmentStatus.OUT_FOR_DELIVERY.canTransitionTo(ShipmentStatus.RETURNED)).isFalse();
        }

        @Test
        @DisplayName("DELIVERED → RETURNED is allowed (post-delivery return path)")
        void delivered_toReturned_allowed() {
            assertThat(ShipmentStatus.DELIVERED.canTransitionTo(ShipmentStatus.RETURNED)).isTrue();
        }

        @Test
        @DisplayName("FAILED and RETURNED are terminal — no transitions allowed")
        void terminal_states_noTransitions() {
            for (ShipmentStatus next : ShipmentStatus.values()) {
                assertThat(ShipmentStatus.FAILED.canTransitionTo(next))
                        .as("FAILED should not transition to %s", next)
                        .isFalse();
                assertThat(ShipmentStatus.RETURNED.canTransitionTo(next))
                        .as("RETURNED should not transition to %s", next)
                        .isFalse();
            }
        }
    }

    @Nested
    @DisplayName("CanTransitionTo — full matrix")
    class CanTransitionTo {

        @Test
        @DisplayName("PENDING: allowed → PICKED_UP, FAILED, RETURNED; blocked → others")
        void pending_matrix() {
            assertThat(ShipmentStatus.PENDING.canTransitionTo(ShipmentStatus.PICKED_UP)).isTrue();
            assertThat(ShipmentStatus.PENDING.canTransitionTo(ShipmentStatus.FAILED)).isTrue();
            assertThat(ShipmentStatus.PENDING.canTransitionTo(ShipmentStatus.RETURNED)).isTrue();

            assertThat(ShipmentStatus.PENDING.canTransitionTo(ShipmentStatus.IN_TRANSIT)).isFalse();
            assertThat(ShipmentStatus.PENDING.canTransitionTo(ShipmentStatus.OUT_FOR_DELIVERY)).isFalse();
            assertThat(ShipmentStatus.PENDING.canTransitionTo(ShipmentStatus.DELIVERED)).isFalse();
        }

        @Test
        @DisplayName("PICKED_UP: allowed → IN_TRANSIT, FAILED; blocked → others")
        void pickedUp_matrix() {
            assertThat(ShipmentStatus.PICKED_UP.canTransitionTo(ShipmentStatus.IN_TRANSIT)).isTrue();
            assertThat(ShipmentStatus.PICKED_UP.canTransitionTo(ShipmentStatus.FAILED)).isTrue();

            assertThat(ShipmentStatus.PICKED_UP.canTransitionTo(ShipmentStatus.PENDING)).isFalse();
            assertThat(ShipmentStatus.PICKED_UP.canTransitionTo(ShipmentStatus.OUT_FOR_DELIVERY)).isFalse();
            assertThat(ShipmentStatus.PICKED_UP.canTransitionTo(ShipmentStatus.DELIVERED)).isFalse();
            assertThat(ShipmentStatus.PICKED_UP.canTransitionTo(ShipmentStatus.RETURNED)).isFalse();
        }

        @Test
        @DisplayName("OUT_FOR_DELIVERY: allowed → DELIVERED, FAILED; blocked → others")
        void outForDelivery_matrix() {
            assertThat(ShipmentStatus.OUT_FOR_DELIVERY.canTransitionTo(ShipmentStatus.DELIVERED)).isTrue();
            assertThat(ShipmentStatus.OUT_FOR_DELIVERY.canTransitionTo(ShipmentStatus.FAILED)).isTrue();

            assertThat(ShipmentStatus.OUT_FOR_DELIVERY.canTransitionTo(ShipmentStatus.PENDING)).isFalse();
            assertThat(ShipmentStatus.OUT_FOR_DELIVERY.canTransitionTo(ShipmentStatus.PICKED_UP)).isFalse();
            assertThat(ShipmentStatus.OUT_FOR_DELIVERY.canTransitionTo(ShipmentStatus.IN_TRANSIT)).isFalse();
            assertThat(ShipmentStatus.OUT_FOR_DELIVERY.canTransitionTo(ShipmentStatus.RETURNED)).isFalse();
        }
    }
}
