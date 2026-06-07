package com.hieu.payment_service.service;

import com.hieu.payment_service.dto.InitiatePaymentRequest;
import com.hieu.payment_service.dto.PaymentDTO;
import com.hieu.payment_service.entity.PaymentJpaEntity;
import com.hieu.payment_service.exception.DuplicatePaymentException;
import com.hieu.payment_service.exception.InvalidPaymentStateException;
import com.hieu.payment_service.exception.PaymentAccessDeniedException;
import com.hieu.payment_service.integration.momo.MomoPayUrlService;
import com.hieu.payment_service.integration.sepay.SepayQrService;
import com.hieu.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link PaymentService} — idempotency, duplicate-order guard, method
 * normalization + provider branching, and the full payment status machine (confirm / cancel /
 * refund-request / refund-process / webhook auto-confirm). Dependencies are mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService (unit)")
class PaymentServiceTest {

    @Mock PaymentRepository repository;
    @Mock SepayQrService sepayQrService;
    @Mock MomoPayUrlService momoPayUrlService;
    @Mock ApplicationEventPublisher eventPublisher;

    PaymentService service;

    @BeforeEach
    void setup() {
        service = new PaymentService(repository, sepayQrService, momoPayUrlService, eventPublisher);
    }

    private static PaymentJpaEntity payment(String id, String userId, String status, BigDecimal amount) {
        var e = new PaymentJpaEntity();
        e.setId(id);
        e.setOrderId("ORD-1");
        e.setUserId(userId);
        e.setAmount(amount);
        e.setCurrency("VND");
        e.setMethod("BANK_TRANSFER");
        e.setStatus(status);
        return e;
    }

    private static InitiatePaymentRequest initRequest(String method, String idempotencyKey) {
        var req = new InitiatePaymentRequest();
        req.setOrderId("ORD-1");
        req.setAmount(BigDecimal.valueOf(100_000));
        req.setCurrency("VND");
        req.setMethod(method);
        req.setIdempotencyKey(idempotencyKey);
        return req;
    }

    @Nested
    @DisplayName("initiatePayment()")
    class Initiate {

        @Test
        @DisplayName("SEPAY alias folds to BANK_TRANSFER and attaches a QR url")
        void initiate_bankTransfer() {
            when(repository.findByOrderIdWithLock("ORD-1")).thenReturn(Optional.empty());
            when(repository.save(any(PaymentJpaEntity.class))).thenAnswer(inv -> {
                PaymentJpaEntity e = inv.getArgument(0);
                e.setId("1");
                return e;
            });
            when(sepayQrService.generateQrUrl("ORD-1", BigDecimal.valueOf(100_000))).thenReturn("http://qr");
            when(sepayQrService.getBankCode()).thenReturn("BIDV");
            when(sepayQrService.getBankAccount()).thenReturn("ACC");
            when(sepayQrService.getAccountName()).thenReturn("NAME");

            PaymentDTO dto = service.initiatePayment("u1", initRequest("SEPAY", null));

            assertThat(dto.getMethod()).isEqualTo("BANK_TRANSFER");
            assertThat(dto.getStatus()).isEqualTo("PENDING");
            assertThat(dto.getQrCodeUrl()).isEqualTo("http://qr");
            assertThat(dto.getTransferContent()).isEqualTo("ORD-1");
        }

        @Test
        @DisplayName("MOMO method attaches a pay url")
        void initiate_momo() {
            when(repository.findByOrderIdWithLock("ORD-1")).thenReturn(Optional.empty());
            when(repository.save(any(PaymentJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(momoPayUrlService.generatePayUrl("ORD-1", BigDecimal.valueOf(100_000))).thenReturn("http://momo");

            PaymentDTO dto = service.initiatePayment("u1", initRequest("MOMO", null));

            assertThat(dto.getMethod()).isEqualTo("MOMO");
            assertThat(dto.getPayUrl()).isEqualTo("http://momo");
        }

        @Test
        @DisplayName("replays the existing payment for a known idempotency key")
        void initiate_idempotentReplay() {
            when(repository.findByIdempotencyKey("key-1"))
                    .thenReturn(Optional.of(payment("9", "u1", "PENDING", BigDecimal.valueOf(100_000))));

            PaymentDTO dto = service.initiatePayment("u1", initRequest("SEPAY", "key-1"));

            assertThat(dto.getId()).isEqualTo("9");
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("rejects a duplicate order")
        void initiate_duplicateOrder() {
            when(repository.findByOrderIdWithLock("ORD-1"))
                    .thenReturn(Optional.of(payment("2", "u1", "PENDING", BigDecimal.valueOf(100_000))));

            assertThatThrownBy(() -> service.initiatePayment("u1", initRequest("SEPAY", null)))
                    .isInstanceOf(DuplicatePaymentException.class);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("rejects an unsupported payment method")
        void initiate_invalidMethod() {
            when(repository.findByOrderIdWithLock("ORD-1")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.initiatePayment("u1", initRequest("CRYPTO", null)))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("confirmPayment()")
    class Confirm {

        @Test
        @DisplayName("PENDING → PAID, records transaction and publishes a completed event")
        void confirm_happyPath() {
            when(repository.findById("1")).thenReturn(Optional.of(payment("1", "u1", "PENDING", BigDecimal.valueOf(100_000))));
            when(repository.save(any(PaymentJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            PaymentDTO dto = service.confirmPayment("1", "u1", "tx-123");

            assertThat(dto.getStatus()).isEqualTo("PAID");
            assertThat(dto.getTransactionId()).isEqualTo("tx-123");
            verify(eventPublisher).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("rejects confirming a non-PENDING payment")
        void confirm_wrongState() {
            when(repository.findById("1")).thenReturn(Optional.of(payment("1", "u1", "PAID", BigDecimal.valueOf(100_000))));

            assertThatThrownBy(() -> service.confirmPayment("1", "u1", "tx"))
                    .isInstanceOf(InvalidPaymentStateException.class);
        }

        @Test
        @DisplayName("rejects confirming another user's payment")
        void confirm_accessDenied() {
            when(repository.findById("1")).thenReturn(Optional.of(payment("1", "u1", "PENDING", BigDecimal.valueOf(100_000))));

            assertThatThrownBy(() -> service.confirmPayment("1", "intruder", "tx"))
                    .isInstanceOf(PaymentAccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("cancelPayment()")
    class Cancel {

        @Test
        @DisplayName("cancels a PENDING payment")
        void cancel_happyPath() {
            when(repository.findById("1")).thenReturn(Optional.of(payment("1", "u1", "PENDING", BigDecimal.valueOf(100_000))));
            when(repository.save(any(PaymentJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            assertThat(service.cancelPayment("1", "u1").getStatus()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("rejects cancelling a PAID payment")
        void cancel_wrongState() {
            when(repository.findById("1")).thenReturn(Optional.of(payment("1", "u1", "PAID", BigDecimal.valueOf(100_000))));

            assertThatThrownBy(() -> service.cancelPayment("1", "u1"))
                    .isInstanceOf(InvalidPaymentStateException.class);
        }
    }

    @Nested
    @DisplayName("requestRefund()")
    class RequestRefund {

        @Test
        @DisplayName("PAID → REFUND_REQUESTED")
        void request_happyPath() {
            when(repository.findById("1")).thenReturn(Optional.of(payment("1", "u1", "PAID", BigDecimal.valueOf(100_000))));
            when(repository.save(any(PaymentJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            assertThat(service.requestRefund("1", "u1", "changed mind").getStatus())
                    .isEqualTo("REFUND_REQUESTED");
        }

        @Test
        @DisplayName("is idempotent when already REFUND_REQUESTED")
        void request_idempotent() {
            when(repository.findById("1"))
                    .thenReturn(Optional.of(payment("1", "u1", "REFUND_REQUESTED", BigDecimal.valueOf(100_000))));

            service.requestRefund("1", "u1", "again");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("rejects refund request for a non-PAID payment")
        void request_wrongState() {
            when(repository.findById("1")).thenReturn(Optional.of(payment("1", "u1", "PENDING", BigDecimal.valueOf(100_000))));

            assertThatThrownBy(() -> service.requestRefund("1", "u1", "x"))
                    .isInstanceOf(InvalidPaymentStateException.class);
        }
    }

    @Nested
    @DisplayName("processRefund()")
    class ProcessRefund {

        @Test
        @DisplayName("REFUND_REQUESTED → REFUNDED with a partial amount and a refunded event")
        void process_partialAmount() {
            when(repository.findById("1"))
                    .thenReturn(Optional.of(payment("1", "u1", "REFUND_REQUESTED", BigDecimal.valueOf(100_000))));
            when(repository.save(any(PaymentJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            PaymentDTO dto = service.processRefund("1", BigDecimal.valueOf(40_000), "partial");

            assertThat(dto.getStatus()).isEqualTo("REFUNDED");
            assertThat(dto.getRefundAmount()).isEqualByComparingTo(BigDecimal.valueOf(40_000));
            verify(eventPublisher).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("defaults the refund amount to the full payment when none is supplied")
        void process_defaultsToFull() {
            when(repository.findById("1"))
                    .thenReturn(Optional.of(payment("1", "u1", "REFUND_REQUESTED", BigDecimal.valueOf(100_000))));
            when(repository.save(any(PaymentJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            PaymentDTO dto = service.processRefund("1", null, null);

            assertThat(dto.getRefundAmount()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
        }

        @Test
        @DisplayName("rejects a refund amount exceeding the original")
        void process_amountTooLarge() {
            when(repository.findById("1"))
                    .thenReturn(Optional.of(payment("1", "u1", "REFUND_REQUESTED", BigDecimal.valueOf(100_000))));

            assertThatThrownBy(() -> service.processRefund("1", BigDecimal.valueOf(200_000), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("is idempotent when already REFUNDED")
        void process_idempotent() {
            when(repository.findById("1"))
                    .thenReturn(Optional.of(payment("1", "u1", "REFUNDED", BigDecimal.valueOf(100_000))));

            service.processRefund("1", BigDecimal.valueOf(50_000), null);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("autoConfirmByOrderId()")
    class AutoConfirm {

        @Test
        @DisplayName("confirms a PENDING payment and publishes a completed event")
        void autoConfirm_pending() {
            when(repository.findByOrderIdWithLock("ORD-1"))
                    .thenReturn(Optional.of(payment("1", "u1", "PENDING", BigDecimal.valueOf(100_000))));
            when(repository.save(any(PaymentJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            service.autoConfirmByOrderId("ORD-1", "sepay-tx");

            verify(eventPublisher).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("is a no-op for an already non-PENDING payment")
        void autoConfirm_alreadyPaid() {
            when(repository.findByOrderIdWithLock("ORD-1"))
                    .thenReturn(Optional.of(payment("1", "u1", "PAID", BigDecimal.valueOf(100_000))));

            service.autoConfirmByOrderId("ORD-1", "sepay-tx");

            verify(repository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any(Object.class));
        }
    }

    @Nested
    @DisplayName("normalizeMethod()")
    class NormalizeMethod {

        @Test
        @DisplayName("folds storefront aliases into the internal method")
        void normalize() {
            assertThat(PaymentService.normalizeMethod("sepay")).isEqualTo("BANK_TRANSFER");
            assertThat(PaymentService.normalizeMethod("VIETQR")).isEqualTo("BANK_TRANSFER");
            assertThat(PaymentService.normalizeMethod(" bank ")).isEqualTo("BANK_TRANSFER");
            assertThat(PaymentService.normalizeMethod("momo")).isEqualTo("MOMO");
            assertThat(PaymentService.normalizeMethod("cod")).isEqualTo("COD");
        }
    }
}
