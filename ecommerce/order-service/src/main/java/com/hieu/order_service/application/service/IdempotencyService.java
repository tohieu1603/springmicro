package com.hieu.order_service.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hieu.order_service.application.dto.OrderDTO;
import com.hieu.order_service.domain.exception.DuplicateOrderException;
import com.hieu.order_service.domain.model.order.IdempotencyRecord;
import com.hieu.order_service.domain.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Redis Lua + DB hybrid idempotency guard for order creation.
 *
 * <ol>
 *   <li>Atomic Redis Lua SETNX: "new" → proceed; "{...}" JSON → return cached DTO; "PROCESSING" → reject.</li>
 *   <li>DB record inserted for durable audit trail.</li>
 *   <li>On success, write full DTO JSON to Redis + update DB record to COMPLETED.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final Duration REDIS_TTL = Duration.ofMinutes(30);
    private static final long REDIS_TTL_SECONDS = REDIS_TTL.toSeconds();
    private static final String REDIS_PREFIX = "order:idem:";
    private static final String PROCESSING_TOKEN = "PROCESSING";

    private final IdempotencyRepository idempotencyRepository;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final DefaultRedisScript<String> claimIdempotencyScript;

    /** Returns Optional.empty() → proceed; Optional.of(dto) → short-circuit. */
    @Transactional
    public Optional<OrderDTO> checkOrCreate(String userId, String key) {
        String redisKey = REDIS_PREFIX + userId + ":" + key;
        String luaResult = claimRedisSlot(redisKey);

        // Step 1 — Redis claim trống → bản ghi này là lần đầu, persist DB audit.
        if (luaResult == null || "new".equals(luaResult)) {
            return persistNewIdempotency(key);
        }
        // Step 2 — JSON cached → trả ngay short-circuit.
        if (luaResult.startsWith("{")) {
            Optional<OrderDTO> cached = parseDto(luaResult, "Redis cache", redisKey);
            if (cached.isPresent()) return cached;
        }
        // Step 3 — PROCESSING → kiểm DB xem có phải stale từ node crash không.
        if (PROCESSING_TOKEN.equals(luaResult)) {
            return resolveProcessingState(key);
        }
        // Fallback — đọc DB record cuối cùng.
        return fallbackToDb(key);
    }

    private String claimRedisSlot(String redisKey) {
        return redis.execute(
                claimIdempotencyScript,
                List.of(redisKey),
                PROCESSING_TOKEN,
                String.valueOf(REDIS_TTL_SECONDS));
    }

    private Optional<OrderDTO> persistNewIdempotency(String key) {
        try {
            idempotencyRepository.save(IdempotencyRecord.create(key));
            return Optional.empty();
        } catch (DataIntegrityViolationException dup) {
            log.warn("Idempotency DB UNIQUE collided after Redis claim for key {} — treating as duplicate", key);
            throw new DuplicateOrderException(key);
        }
    }

    private Optional<OrderDTO> resolveProcessingState(String key) {
        return idempotencyRepository.findByKey(key)
                .map(row -> {
                    if (isCompletedWithBody(row)) {
                        return parseDto(row.getResponseBody(), "DB record", key).orElse(null);
                    }
                    return null;
                })
                .map(Optional::of)
                .orElseThrow(() -> new DuplicateOrderException(key));
    }

    private Optional<OrderDTO> fallbackToDb(String key) {
        var existing = idempotencyRepository.findByKey(key);
        if (existing.isEmpty()) {
            idempotencyRepository.save(IdempotencyRecord.create(key));
            return Optional.empty();
        }
        if (isCompletedWithBody(existing.get())) {
            return parseDto(existing.get().getResponseBody(), "DB record", key);
        }
        return Optional.empty();
    }

    private static boolean isCompletedWithBody(IdempotencyRecord r) {
        return r.getStatus() == IdempotencyRecord.Status.COMPLETED && r.getResponseBody() != null;
    }

    private Optional<OrderDTO> parseDto(String json, String src, String key) {
        try {
            return Optional.of(objectMapper.readValue(json, OrderDTO.class));
        } catch (Exception e) {
            log.warn("{} body corrupt for key {}", src, key);
            return Optional.empty();
        }
    }

    @Transactional
    public void markCompleted(String key, String orderId, OrderDTO dto) {
        idempotencyRepository.findByKey(key).ifPresent(row -> {
            try {
                var json = objectMapper.writeValueAsString(dto);
                row.markCompleted(orderId, json);
                idempotencyRepository.save(row);
                var redisKey = REDIS_PREFIX + dto.userId() + ":" + key;
                cacheToRedis(redisKey, json);
            } catch (Exception e) {
                log.warn("Failed to persist idempotency completion for key {}: {}", key, e.getMessage());
            }
        });
    }

    private void cacheToRedis(String redisKey, String json) {
        try {
            redis.opsForValue().set(redisKey, json, REDIS_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache idempotency to Redis key {}: {}", redisKey, e.getMessage());
        }
    }
}
