package com.hieu.shipping_service.service;

import com.hieu.shipping_service.dto.*;
import com.hieu.shipping_service.entity.Carrier;
import com.hieu.shipping_service.entity.ShipmentJpaEntity;
import com.hieu.shipping_service.entity.ShipmentStatus;
import com.hieu.shipping_service.exception.DuplicateShipmentException;
import com.hieu.shipping_service.exception.InvalidShipmentStateException;
import com.hieu.shipping_service.exception.ShipmentAccessDeniedException;
import com.hieu.shipping_service.exception.ShipmentNotFoundException;
import com.hieu.shipping_service.kafka.ShipmentDeliveredEvent;
import com.hieu.shipping_service.kafka.ShipmentStatusChangedEvent;
import com.hieu.shipping_service.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Core shipment business logic. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository repo;
    private final ApplicationEventPublisher events;

    /** Creates a new PENDING shipment. ADMIN or internal only. */
    @Transactional
    public ShipmentDTO createShipment(CreateShipmentRequest req) {
        if (repo.findByOrderId(req.orderId()).isPresent()) {
            throw new DuplicateShipmentException(req.orderId());
        }
        if (req.carrier() != null) validateCarrier(req.carrier());

        var entity = new ShipmentJpaEntity();
        entity.setOrderId(req.orderId());
        entity.setUserId(req.userId());
        entity.setStatus(ShipmentStatus.PENDING.name());
        entity.setRecipientName(req.recipientName());
        entity.setRecipientPhone(req.recipientPhone());
        entity.setAddressLine(req.addressLine());
        entity.setCity(req.city());
        entity.setCountry(req.country() != null ? req.country() : "Vietnam");
        if (req.carrier()  != null) entity.setCarrier(req.carrier());
        if (req.ward()     != null) entity.setWard(req.ward());
        if (req.district() != null) entity.setDistrict(req.district());
        if (req.notes()    != null) entity.setNotes(req.notes());

        var saved = repo.save(entity);
        log.debug("Created shipment {} for orderId={}", saved.getId(), req.orderId());
        return toDTO(saved);
    }

    /** Idempotent shipment creation from Kafka consumer — swallows duplicates. */
    @Transactional
    public void createShipmentIfAbsent(CreateShipmentRequest req) {
        if (repo.findByOrderId(req.orderId()).isPresent()) {
            log.debug("Shipment already exists for orderId={}, skipping", req.orderId());
            return;
        }
        // C1: Catch concurrent-insert race (two consumers read "absent" before either commits).
        try {
            createShipment(req);
        } catch (DataIntegrityViolationException dup) {
            log.info("Shipment for order {} already exists (concurrent insert)", req.orderId());
            // Idempotent path — existing shipment is the correct result; no further action needed.
        }
    }

    @Transactional(readOnly = true)
    public ShipmentDTO getShipmentForUser(String id, String userId, boolean isAdmin) {
        var entity = findById(id);
        if (!isAdmin && !entity.getUserId().equals(userId)) {
            throw new ShipmentAccessDeniedException(id);
        }
        return toDTO(entity);
    }

    @Transactional(readOnly = true)
    public ShipmentDTO getShipmentByOrderForUser(String orderId, String userId, boolean isAdmin) {
        var entity = repo.findByOrderId(orderId)
                .orElseThrow(() -> new ShipmentNotFoundException("order: " + orderId));
        if (!isAdmin && !entity.getUserId().equals(userId)) {
            throw new ShipmentAccessDeniedException(entity.getId());
        }
        return toDTO(entity);
    }

    @Transactional(readOnly = true)
    public TrackingInfoDTO getTrackingInfo(String trackingNumber) {
        var entity = repo.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ShipmentNotFoundException("tracking: " + trackingNumber));
        return new TrackingInfoDTO(
                entity.getTrackingNumber(),
                entity.getCarrier(),
                entity.getStatus(),
                entity.getCity(),
                entity.getEstimatedDeliveryDate(),
                entity.getActualDeliveryDate());
    }

    @Transactional(readOnly = true)
    public List<ShipmentDTO> getMyShipments(String userId) {
        return repo.findByUserId(userId).stream().map(this::toDTO).toList();
    }

    /** ADMIN: update status — validates state machine. */
    @Transactional
    public ShipmentDTO updateStatus(String id, String newStatusStr, String notes) {
        var entity = findById(id);
        var current = ShipmentStatus.valueOf(entity.getStatus());
        var target  = parseStatus(newStatusStr);
        if (!current.canTransitionTo(target)) {
            throw new InvalidShipmentStateException(
                    "Cannot transition from %s to %s".formatted(current, target));
        }
        var oldStatus = entity.getStatus();
        entity.setStatus(target.name());
        if (notes != null) entity.setNotes(notes);
        var saved = repo.save(entity);

        events.publishEvent(ShipmentStatusChangedEvent.of(
                saved.getId(), saved.getOrderId(), saved.getUserId(),
                oldStatus, target.name(), saved.getTrackingNumber()));

        log.debug("Shipment {} status {} -> {}", id, oldStatus, target);
        return toDTO(saved);
    }

    /** ADMIN: assign tracking number + carrier. */
    @Transactional
    public ShipmentDTO assignTracking(String id, String carrier, String trackingNumber) {
        validateCarrier(carrier);
        var entity = findById(id);
        var current = ShipmentStatus.valueOf(entity.getStatus());
        if (current == ShipmentStatus.DELIVERED || current == ShipmentStatus.RETURNED
                || current == ShipmentStatus.FAILED) {
            throw new IllegalStateException("Cannot modify shipment in terminal state: " + current);
        }
        repo.findByTrackingNumber(trackingNumber).ifPresent(existing -> {
            if (!id.equals(existing.getId())) {
                throw new IllegalArgumentException(
                        "Tracking number already used by shipment " + existing.getId());
            }
        });
        entity.setCarrier(carrier);
        entity.setTrackingNumber(trackingNumber);
        try {
            var saved = repo.save(entity);
            log.debug("Assigned tracking {} ({}) to shipment {}", trackingNumber, carrier, id);
            return toDTO(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Tracking number already in use (concurrent): " + trackingNumber);
        }
    }

    /** ADMIN: set estimated delivery date. */
    @Transactional
    public ShipmentDTO setEstimatedDelivery(String id, Instant estimatedDate) {
        var entity = findById(id);
        var current = ShipmentStatus.valueOf(entity.getStatus());
        if (current == ShipmentStatus.DELIVERED || current == ShipmentStatus.RETURNED
                || current == ShipmentStatus.FAILED) {
            throw new IllegalStateException("Cannot modify shipment in terminal state: " + current);
        }
        entity.setEstimatedDeliveryDate(estimatedDate);
        return toDTO(repo.save(entity));
    }

    /** ADMIN: OUT_FOR_DELIVERY → DELIVERED; sets actual_delivery_date. */
    @Transactional
    public ShipmentDTO markDelivered(String id) {
        var entity = findById(id);
        var current = ShipmentStatus.valueOf(entity.getStatus());
        if (!current.canTransitionTo(ShipmentStatus.DELIVERED)) {
            throw new InvalidShipmentStateException(
                    "Cannot mark DELIVERED from state: " + current + ". Must be OUT_FOR_DELIVERY.");
        }
        var now = Instant.now();
        var oldStatus = entity.getStatus();
        entity.setStatus(ShipmentStatus.DELIVERED.name());
        entity.setActualDeliveryDate(now);

        if (entity.getEstimatedDeliveryDate() != null) {
            var delay = Duration.between(entity.getEstimatedDeliveryDate(), now);
            if (delay.toHours() > 24) {
                log.warn("Shipment {} delivered {} h late (est={}, actual={})",
                        id, delay.toHours(), entity.getEstimatedDeliveryDate(), now);
            }
        }
        var saved = repo.save(entity);

        events.publishEvent(ShipmentStatusChangedEvent.of(
                saved.getId(), saved.getOrderId(), saved.getUserId(),
                oldStatus, ShipmentStatus.DELIVERED.name(), saved.getTrackingNumber()));
        events.publishEvent(ShipmentDeliveredEvent.of(
                saved.getId(), saved.getOrderId(), saved.getUserId(), now));

        log.debug("Shipment {} marked DELIVERED", id);
        return toDTO(saved);
    }

    /** ADMIN: paginated list by status. */
    @Transactional(readOnly = true)
    public Page<ShipmentDTO> listByStatus(String statusStr, int page, int size) {
        parseStatus(statusStr); // validate
        return repo.findByStatus(statusStr, PageRequest.of(page, size)).map(this::toDTO);
    }

    // --- helpers ---

    private ShipmentJpaEntity findById(String id) {
        return repo.findById(id).orElseThrow(() -> new ShipmentNotFoundException(id));
    }

    private static ShipmentStatus parseStatus(String s) {
        try {
            return ShipmentStatus.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid shipment status: " + s);
        }
    }

    private static void validateCarrier(String c) {
        try {
            Carrier.valueOf(c);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid carrier: " + c);
        }
    }

    private ShipmentDTO toDTO(ShipmentJpaEntity e) {
        return new ShipmentDTO(
                e.getId(), e.getOrderId(), e.getUserId(),
                e.getCarrier(), e.getTrackingNumber(), e.getStatus(),
                e.getRecipientName(), e.getRecipientPhone(), e.getAddressLine(),
                e.getWard(), e.getDistrict(), e.getCity(), e.getCountry(),
                e.getEstimatedDeliveryDate(), e.getActualDeliveryDate(),
                e.getNotes(), e.getCreatedAt(), e.getUpdatedAt(), e.getVersion());
    }
}
