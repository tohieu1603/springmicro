package com.hieu.shipping_service.service;

import com.hieu.shipping_service.dto.CreateShipmentRequest;
import com.hieu.shipping_service.dto.ShipmentDTO;
import com.hieu.shipping_service.entity.ShipmentJpaEntity;
import com.hieu.shipping_service.entity.ShipmentStatus;
import com.hieu.shipping_service.exception.DuplicateShipmentException;
import com.hieu.shipping_service.exception.InvalidShipmentStateException;
import com.hieu.shipping_service.exception.ShipmentAccessDeniedException;
import com.hieu.shipping_service.kafka.ShipmentDeliveredEvent;
import com.hieu.shipping_service.kafka.ShipmentStatusChangedEvent;
import com.hieu.shipping_service.repository.ShipmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link ShipmentService} — creation guards, the status state machine,
 * tracking assignment rules, delivery, and per-user access control. Repository and event
 * publisher are mocked; no Spring, no DB.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ShipmentService (unit)")
class ShipmentServiceTest {

    @Mock ShipmentRepository repo;
    @Mock ApplicationEventPublisher events;

    ShipmentService service;

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        service = new ShipmentService(repo, events);
    }

    private static ShipmentJpaEntity shipment(String id, String userId, ShipmentStatus status) {
        var e = new ShipmentJpaEntity();
        e.setId(id);
        e.setOrderId("ORD-1");
        e.setUserId(userId);
        e.setStatus(status.name());
        e.setRecipientName("Recipient");
        e.setRecipientPhone("0900000000");
        e.setAddressLine("123 Street");
        e.setCity("HCM");
        e.setCountry("Vietnam");
        return e;
    }

    private static CreateShipmentRequest createRequest(String carrier, String country) {
        return new CreateShipmentRequest(
                "ORD-1", "u1", carrier, "Recipient", "0900000000",
                "123 Street", "Ward", "District", "HCM", country, "note");
    }

    @Nested
    @DisplayName("createShipment()")
    class Create {

        @Test
        @DisplayName("creates a PENDING shipment and defaults country to Vietnam")
        void create_happyPath() {
            when(repo.findByOrderId("ORD-1")).thenReturn(Optional.empty());
            when(repo.save(any(ShipmentJpaEntity.class))).thenAnswer(inv -> {
                ShipmentJpaEntity e = inv.getArgument(0);
                e.setId("uuid-7");
                return e;
            });

            ShipmentDTO dto = service.createShipment(createRequest("GHTK", null));

            assertThat(dto.status()).isEqualTo(ShipmentStatus.PENDING.name());
            assertThat(dto.country()).isEqualTo("Vietnam");
            verify(repo).save(any(ShipmentJpaEntity.class));
        }

        @Test
        @DisplayName("rejects a duplicate orderId")
        void create_duplicate() {
            when(repo.findByOrderId("ORD-1")).thenReturn(Optional.of(shipment("uuid-1", "u1", ShipmentStatus.PENDING)));

            assertThatThrownBy(() -> service.createShipment(createRequest("GHTK", "Vietnam")))
                    .isInstanceOf(DuplicateShipmentException.class);
            verify(repo, never()).save(any());
        }

        @Test
        @DisplayName("rejects an unknown carrier")
        void create_invalidCarrier() {
            when(repo.findByOrderId("ORD-1")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createShipment(createRequest("FEDEX", "Vietnam")))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(repo, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatus {

        @Test
        @DisplayName("performs a legal transition and publishes a status-changed event")
        void update_legalTransition() {
            when(repo.findById("uuid-1")).thenReturn(Optional.of(shipment("uuid-1", "u1", ShipmentStatus.PENDING)));
            when(repo.save(any(ShipmentJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            ShipmentDTO dto = service.updateStatus("uuid-1", "PICKED_UP", "picked");

            assertThat(dto.status()).isEqualTo("PICKED_UP");
            verify(events).publishEvent(any(ShipmentStatusChangedEvent.class));
        }

        @Test
        @DisplayName("rejects an illegal transition")
        void update_illegalTransition() {
            when(repo.findById("uuid-1")).thenReturn(Optional.of(shipment("uuid-1", "u1", ShipmentStatus.PENDING)));

            assertThatThrownBy(() -> service.updateStatus("uuid-1", "DELIVERED", null))
                    .isInstanceOf(InvalidShipmentStateException.class);
            verify(repo, never()).save(any());
            verify(events, never()).publishEvent(any());
        }

        @Test
        @DisplayName("rejects an unknown status string")
        void update_invalidStatus() {
            when(repo.findById("uuid-1")).thenReturn(Optional.of(shipment("uuid-1", "u1", ShipmentStatus.PENDING)));

            assertThatThrownBy(() -> service.updateStatus("uuid-1", "BOGUS", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("assignTracking()")
    class AssignTracking {

        @Test
        @DisplayName("assigns carrier + tracking number on a non-terminal shipment")
        void assign_happyPath() {
            when(repo.findById("uuid-1")).thenReturn(Optional.of(shipment("uuid-1", "u1", ShipmentStatus.IN_TRANSIT)));
            when(repo.findByTrackingNumber("TRACK-1")).thenReturn(Optional.empty());
            when(repo.save(any(ShipmentJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            ShipmentDTO dto = service.assignTracking("uuid-1", "GHTK", "TRACK-1");

            assertThat(dto.trackingNumber()).isEqualTo("TRACK-1");
            assertThat(dto.carrier()).isEqualTo("GHTK");
        }

        @Test
        @DisplayName("rejects assigning tracking to a terminal shipment")
        void assign_terminalState() {
            when(repo.findById("uuid-1")).thenReturn(Optional.of(shipment("uuid-1", "u1", ShipmentStatus.DELIVERED)));

            assertThatThrownBy(() -> service.assignTracking("uuid-1", "GHTK", "TRACK-1"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("rejects a tracking number already used by another shipment")
        void assign_trackingTaken() {
            when(repo.findById("uuid-1")).thenReturn(Optional.of(shipment("uuid-1", "u1", ShipmentStatus.IN_TRANSIT)));
            when(repo.findByTrackingNumber("TRACK-1"))
                    .thenReturn(Optional.of(shipment("uuid-2", "u1", ShipmentStatus.IN_TRANSIT)));

            assertThatThrownBy(() -> service.assignTracking("uuid-1", "GHTK", "TRACK-1"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("markDelivered()")
    class MarkDelivered {

        @Test
        @DisplayName("OUT_FOR_DELIVERY → DELIVERED sets actual date and emits two events")
        void delivered_happyPath() {
            when(repo.findById("uuid-1")).thenReturn(Optional.of(shipment("uuid-1", "u1", ShipmentStatus.OUT_FOR_DELIVERY)));
            when(repo.save(any(ShipmentJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            ShipmentDTO dto = service.markDelivered("uuid-1");

            assertThat(dto.status()).isEqualTo(ShipmentStatus.DELIVERED.name());
            assertThat(dto.actualDeliveryDate()).isNotNull();
            verify(events).publishEvent(any(ShipmentStatusChangedEvent.class));
            verify(events).publishEvent(any(ShipmentDeliveredEvent.class));
        }

        @Test
        @DisplayName("rejects delivery from a non OUT_FOR_DELIVERY state")
        void delivered_illegalState() {
            when(repo.findById("uuid-1")).thenReturn(Optional.of(shipment("uuid-1", "u1", ShipmentStatus.PENDING)));

            assertThatThrownBy(() -> service.markDelivered("uuid-1"))
                    .isInstanceOf(InvalidShipmentStateException.class);
        }
    }

    @Nested
    @DisplayName("getShipmentForUser() — access control")
    class AccessControl {

        @Test
        @DisplayName("owner can read their shipment")
        void owner_allowed() {
            when(repo.findById("uuid-1")).thenReturn(Optional.of(shipment("uuid-1", "u1", ShipmentStatus.PENDING)));
            assertThat(service.getShipmentForUser("uuid-1", "u1", false)).isNotNull();
        }

        @Test
        @DisplayName("a different non-admin user is denied")
        void otherUser_denied() {
            when(repo.findById("uuid-1")).thenReturn(Optional.of(shipment("uuid-1", "u1", ShipmentStatus.PENDING)));
            assertThatThrownBy(() -> service.getShipmentForUser("uuid-1", "u2", false))
                    .isInstanceOf(ShipmentAccessDeniedException.class);
        }

        @Test
        @DisplayName("an admin can read any shipment")
        void admin_allowed() {
            when(repo.findById("uuid-1")).thenReturn(Optional.of(shipment("uuid-1", "u1", ShipmentStatus.PENDING)));
            assertThat(service.getShipmentForUser("uuid-1", "someone-else", true)).isNotNull();
        }
    }
}
