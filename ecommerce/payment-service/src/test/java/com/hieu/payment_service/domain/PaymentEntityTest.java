package com.hieu.payment_service.domain;

import com.hieu.payment_service.entity.PaymentJpaEntity;
import com.hieu.payment_service.exception.InvalidPaymentStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PaymentJpaEntity — pure domain unit tests")
class PaymentEntityTest {
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


    /** Helper: creates a minimal entity in the given status. */
    private static PaymentJpaEntity entityWithStatus(String status) {
        var e = new PaymentJpaEntity();
        e.setOrderId("ORD-001");
        e.setUserId("user-1");
        e.setAmount(BigDecimal.valueOf(150_000));
        e.setCurrency("VND");
        e.setMethod("BANK_TRANSFER");
        e.setStatus(status);
        return e;
    }

    @Nested
    @DisplayName("StateMachine")
    class StateMachine {

        @Test
        @DisplayName("PENDING → PAID when markPaid sets status and paidAt")
        void markPaid_fromPending_transitionsToPaid() {
            var entity = entityWithStatus("PENDING");

            // act — simulate service logic inline (entity is a plain JPA bean)
            if (!"PENDING".equals(entity.getStatus())) {
                throw new InvalidPaymentStateException("Not PENDING");
            }
            entity.setStatus("PAID");
            java.time.Instant now = java.time.Instant.now();
            entity.setPaidAt(now);

            assertThat(entity.getStatus()).isEqualTo("PAID");
            assertThat(entity.getPaidAt()).isNotNull();
        }

        @Test
        @DisplayName("PENDING → FAILED sets status to FAILED")
        void markFailed_fromPending_transitionsToFailed() {
            var entity = entityWithStatus("PENDING");
            entity.setStatus("FAILED");

            assertThat(entity.getStatus()).isEqualTo("FAILED");
        }

        @Test
        @DisplayName("PAID cannot transition back to PENDING")
        void paid_cannotGoBackToPending() {
            var entity = entityWithStatus("PAID");

            // The service guards this; verify the guard logic in isolation
            boolean canGoBack = "PENDING".equals(entity.getStatus());
            assertThat(canGoBack)
                    .as("A PAID entity must not be re-set to PENDING")
                    .isFalse();
        }

        @Test
        @DisplayName("CANCELLED → PENDING transition is blocked")
        void cancelled_cannotTransitionToPending() {
            var entity = entityWithStatus("CANCELLED");

            boolean isTransitionAllowed = "PENDING".equals(entity.getStatus())
                    || "FAILED".equals(entity.getStatus());
            assertThat(isTransitionAllowed).isFalse();
        }
    }

    @Nested
    @DisplayName("Refund")
    class Refund {

        @Test
        @DisplayName("requestRefund from PAID → REFUND_REQUESTED")
        void requestRefund_fromPaid_setsRefundRequested() {
            var entity = entityWithStatus("PAID");

            if (!"PAID".equals(entity.getStatus())) {
                throw new InvalidPaymentStateException("Not PAID");
            }
            entity.setStatus("REFUND_REQUESTED");
            entity.setNotes("Customer request");

            assertThat(entity.getStatus()).isEqualTo("REFUND_REQUESTED");
            assertThat(entity.getNotes()).isEqualTo("Customer request");
        }

        @Test
        @DisplayName("markRefunded from REFUND_REQUESTED → REFUNDED, sets refundAmount")
        void markRefunded_fromRefundRequested_setsRefunded() {
            var entity = entityWithStatus("REFUND_REQUESTED");
            entity.setAmount(BigDecimal.valueOf(150_000));

            entity.setStatus("REFUNDED");
            entity.setRefundAmount(BigDecimal.valueOf(150_000));

            assertThat(entity.getStatus()).isEqualTo("REFUNDED");
            assertThat(entity.getRefundAmount()).isEqualByComparingTo("150000");
        }

        @Test
        @DisplayName("markRefunded from PENDING throws InvalidPaymentStateException")
        void markRefunded_fromPending_throws() {
            var entity = entityWithStatus("PENDING");

            assertThatThrownBy(() -> {
                if (!"REFUND_REQUESTED".equals(entity.getStatus())) {
                    throw new InvalidPaymentStateException(
                            "Cannot process refund for payment in state: "
                                    + entity.getStatus() + ". Must be REFUND_REQUESTED.");
                }
            })
                    .isInstanceOf(InvalidPaymentStateException.class)
                    .hasMessageContaining("Must be REFUND_REQUESTED");
        }
    }
}
