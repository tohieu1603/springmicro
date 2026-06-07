package com.hieu.voucher_service.service;

import com.hieu.voucher_service.dto.ApplyVoucherResponse;
import com.hieu.voucher_service.dto.CreateVoucherRequest;
import com.hieu.voucher_service.dto.UpdateVoucherRequest;
import com.hieu.voucher_service.dto.VoucherDTO;
import com.hieu.voucher_service.entity.VoucherJpaEntity;
import com.hieu.voucher_service.entity.VoucherUsageRecord;
import com.hieu.voucher_service.exception.DuplicateVoucherException;
import com.hieu.voucher_service.exception.VoucherExpiredException;
import com.hieu.voucher_service.exception.VoucherInactiveException;
import com.hieu.voucher_service.exception.VoucherMinOrderException;
import com.hieu.voucher_service.exception.VoucherNotFoundException;
import com.hieu.voucher_service.exception.VoucherUsageLimitException;
import com.hieu.voucher_service.repository.VoucherJpaRepository;
import com.hieu.voucher_service.repository.VoucherUsageRecordRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherApplicationService {

    private static final String PERCENTAGE = "PERCENTAGE";

    private final VoucherJpaRepository voucherRepository;
    private final VoucherUsageRecordRepository usageRecordRepository;
    private final EntityManager entityManager;

    @Transactional
    public VoucherDTO createVoucher(CreateVoucherRequest req) {
        String code = req.getCode().trim().toUpperCase();

        if (PERCENTAGE.equals(req.getType())
                && req.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Percentage discount cannot exceed 100");
        }
        if (req.getStartDate() != null && req.getEndDate() != null
                && !req.getStartDate().isBefore(req.getEndDate())) {
            throw new IllegalArgumentException("startDate must be before endDate");
        }

        VoucherJpaEntity entity = new VoucherJpaEntity();
        entity.setCode(code);
        entity.setType(req.getType());
        entity.setDiscountValue(req.getDiscountValue());
        entity.setMinOrderAmount(req.getMinOrderAmount());
        entity.setMaxDiscountAmount(req.getMaxDiscountAmount());
        entity.setUsageLimit(req.getUsageLimit());
        entity.setUsageLimitPerUser(req.getUsageLimitPerUser());
        entity.setStartDate(req.getStartDate());
        entity.setEndDate(req.getEndDate());
        entity.setDescription(req.getDescription());

        try {
            VoucherJpaEntity saved = voucherRepository.save(entity);
            log.debug("Created voucher {} id={}", saved.getCode(), saved.getId());
            return toDTO(saved);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new DuplicateVoucherException(code);
        }
    }

    @Transactional(readOnly = true)
    public VoucherDTO getVoucher(String id) {
        return toDTO(findById(id));
    }

    @Transactional(readOnly = true)
    public VoucherDTO getVoucherByCode(String code) {
        return toDTO(findByCode(code));
    }

    @Transactional(readOnly = true)
    public Page<VoucherDTO> listVouchers(int page, int size) {
        return voucherRepository.findAll(PageRequest.of(page, size)).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<VoucherDTO> listActiveVouchers(int page, int size) {
        // Filter by time window so expired vouchers are excluded even if active=true
        return voucherRepository.findActiveAtTime(Instant.now(), PageRequest.of(page, size)).map(this::toDTO);
    }

    @Transactional
    public VoucherDTO updateVoucher(String id, UpdateVoucherRequest req) {
        VoucherJpaEntity entity = findById(id);

        if (req.getType() != null) entity.setType(req.getType());
        if (req.getDiscountValue() != null) entity.setDiscountValue(req.getDiscountValue());
        if (req.getMinOrderAmount() != null) entity.setMinOrderAmount(req.getMinOrderAmount());
        if (req.getMaxDiscountAmount() != null) entity.setMaxDiscountAmount(req.getMaxDiscountAmount());
        if (req.getUsageLimit() != null) entity.setUsageLimit(req.getUsageLimit());
        if (req.getStartDate() != null) entity.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) entity.setEndDate(req.getEndDate());
        if (req.getActive() != null) entity.setActive(req.getActive());
        if (req.getDescription() != null) entity.setDescription(req.getDescription());

        VoucherJpaEntity saved = voucherRepository.save(entity);
        log.debug("Updated voucher id={}", id);
        return toDTO(saved);
    }

    @Transactional
    public VoucherDTO deactivateVoucher(String id) {
        VoucherJpaEntity entity = findById(id);
        entity.setActive(false);
        VoucherJpaEntity saved = voucherRepository.save(entity);
        log.debug("Deactivated voucher id={}", id);
        return toDTO(saved);
    }

    /**
     * Validate và apply voucher trong một pessimistic-locked transaction.
     * userId là String (UUID) để match auth-service/order-service.
     * productIds là List<String> để match catalog-service.
     */
    @Transactional
    public ApplyVoucherResponse validateAndApply(
            String code, BigDecimal orderAmount, String userId, String orderId, List<String> productIds) {

        // Pessimistic write lock trước khi đọc usedCount
        VoucherJpaEntity entity = voucherRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> new VoucherNotFoundException(code));

        // Idempotency: saga retry with same orderId must return the previously-applied
        // discount, not increment usedCount again (would hit DB unique constraint and 500).
        // Check INSIDE the pessimistic-lock scope so concurrent first-time apply on the
        // same orderId is still serialized.
        var existing = usageRecordRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            if (!existing.get().getVoucherId().equals(entity.getId())) {
                throw new IllegalArgumentException(
                    "orderId " + orderId + " already used with a different voucher");
            }
            BigDecimal cachedDiscount = calculateDiscount(entity, orderAmount);
            BigDecimal cachedFinal = orderAmount.subtract(cachedDiscount).max(BigDecimal.ZERO);
            log.debug("Idempotent validateAndApply for orderId={}, returning cached discount", orderId);
            return ApplyVoucherResponse.builder()
                    .code(code).discountAmount(cachedDiscount).finalAmount(cachedFinal)
                    .message("Voucher already applied (idempotent)").build();
        }

        // Tất cả guard kiểm tra hợp lệ được gom vào helper để giữ cognitive
        // complexity của method chính ở mức dễ đọc.
        assertVoucherApplicable(entity, code, orderAmount, userId, productIds);

        BigDecimal discountAmount = calculateDiscount(entity, orderAmount);

        // Increment dưới pessimistic lock
        entity.setUsedCount(entity.getUsedCount() + 1);
        voucherRepository.save(entity);

        // Flush ngay để DB write xảy ra trong scope của lock
        entityManager.flush();

        // Ghi usage record
        usageRecordRepository.save(new VoucherUsageRecord(entity.getId(), userId, orderId));

        BigDecimal finalAmount = orderAmount.subtract(discountAmount).max(BigDecimal.ZERO);
        log.debug("Applied voucher {} to order {} for user {}; discount={}", code, orderId, userId, discountAmount);

        return ApplyVoucherResponse.builder()
                .code(code)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .message("Voucher applied successfully")
                .build();
    }

    /**
     * Release voucher khi order bị cancel. Idempotent — nếu không có record thì skip.
     */
    @Transactional
    public void releaseVoucher(String code, String orderId) {
        usageRecordRepository.findByOrderId(orderId).ifPresentOrElse(
            record -> {
                // Use pessimistic write lock to prevent race with concurrent validateAndApply
                VoucherJpaEntity entity = voucherRepository.findByCodeForUpdate(code)
                        .orElseThrow(() -> new VoucherNotFoundException(code));
                // Verify the caller-supplied code actually matches the voucher recorded for
                // this orderId — without this check, a caller can release some OTHER voucher
                // (decrement its usedCount + delete this orderId's record).
                if (!entity.getId().equals(record.getVoucherId())) {
                    throw new IllegalArgumentException(
                        "Code " + code + " does not match the voucher used by orderId " + orderId);
                }
                if (entity.getUsedCount() > 0) {
                    entity.setUsedCount(entity.getUsedCount() - 1);
                    voucherRepository.save(entity);
                    log.debug("Released voucher {} orderId={}; usedCount now={}", code, orderId, entity.getUsedCount());
                } else {
                    log.warn("releaseVoucher: usedCount already 0 for code={} orderId={}", code, orderId);
                }
                usageRecordRepository.deleteByOrderId(orderId);
            },
            () -> log.warn("releaseVoucher: no usage record for orderId={}; skipping (idempotent)", orderId)
        );
    }

    // --- private helpers ---

    private VoucherJpaEntity findById(String id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new VoucherNotFoundException(id));
    }

    private VoucherJpaEntity findByCode(String code) {
        return voucherRepository.findByCode(code)
                .orElseThrow(() -> new VoucherNotFoundException(code));
    }

    /**
     * Tổng hợp toàn bộ guard kiểm tra hợp lệ (active, window, usage limit,
     * targetUserIds, applicableProductIds, per-user limit, minOrderAmount).
     * Tách khỏi {@link #validateAndApply} để giảm cognitive complexity.
     */
    private void assertVoucherApplicable(
            VoucherJpaEntity entity, String code, BigDecimal orderAmount,
            String userId, List<String> productIds) {

        if (!entity.isActive()) throw new VoucherInactiveException(code);

        Instant now = Instant.now();
        boolean notStarted = entity.getStartDate() != null && now.isBefore(entity.getStartDate());
        boolean ended     = entity.getEndDate()   != null && now.isAfter(entity.getEndDate());
        if (notStarted || ended) throw new VoucherExpiredException(code);

        if (entity.getUsageLimit() != null && entity.getUsedCount() >= entity.getUsageLimit()) {
            throw new VoucherUsageLimitException(code);
        }

        assertUserAllowed(entity, code, userId);
        assertProductsAllowed(entity, code, productIds);
        assertPerUserLimitOk(entity, code, userId);

        if (entity.getMinOrderAmount() != null
                && orderAmount.compareTo(entity.getMinOrderAmount()) < 0) {
            throw new VoucherMinOrderException(code, entity.getMinOrderAmount());
        }
    }

    private void assertUserAllowed(VoucherJpaEntity entity, String code, String userId) {
        String csv = entity.getTargetUserIds();
        if (csv == null || csv.isBlank()) return;
        if (!parseCsvStrings(csv).contains(userId)) {
            throw new IllegalArgumentException(
                "Voucher '" + code + "' is not applicable for this user");
        }
    }

    private void assertProductsAllowed(VoucherJpaEntity entity, String code, List<String> productIds) {
        String csv = entity.getApplicableProductIds();
        if (csv == null || csv.isBlank()) return;
        List<String> allowed = parseCsvStrings(csv);
        boolean hasOverlap = productIds != null && productIds.stream().anyMatch(allowed::contains);
        if (!hasOverlap) {
            throw new IllegalArgumentException(
                "Voucher '" + code + "' is not applicable for any product in this order");
        }
    }

    private void assertPerUserLimitOk(VoucherJpaEntity entity, String code, String userId) {
        Integer limit = entity.getUsageLimitPerUser();
        if (limit == null) return;
        long used = usageRecordRepository.countByVoucherIdAndUserId(entity.getId(), userId);
        if (used >= limit) {
            throw new VoucherUsageLimitException(
                "Voucher usage limit per user exceeded for '" + code + "'");
        }
    }

    private List<String> parseCsvStrings(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private BigDecimal calculateDiscount(VoucherJpaEntity entity, BigDecimal orderAmount) {
        if (PERCENTAGE.equals(entity.getType())) {
            BigDecimal discount = orderAmount
                    .multiply(entity.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (entity.getMaxDiscountAmount() != null) {
                discount = discount.min(entity.getMaxDiscountAmount());
            }
            return discount;
        } else {
            // FIXED_AMOUNT: discount không vượt quá order amount
            return entity.getDiscountValue().min(orderAmount);
        }
    }

    private VoucherDTO toDTO(VoucherJpaEntity e) {
        return VoucherDTO.builder()
                .id(e.getId())
                .code(e.getCode())
                .type(e.getType())
                .discountValue(e.getDiscountValue())
                .minOrderAmount(e.getMinOrderAmount())
                .maxDiscountAmount(e.getMaxDiscountAmount())
                .usageLimit(e.getUsageLimit())
                .usedCount(e.getUsedCount())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .active(e.isActive())
                .description(e.getDescription())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
