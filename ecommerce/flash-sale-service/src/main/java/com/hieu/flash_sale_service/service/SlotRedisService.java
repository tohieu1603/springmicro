package com.hieu.flash_sale_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Handles atomic Redis slot management via Lua script.
 * Key pattern: {@code flashsale:slots:{saleId}}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlotRedisService {

    private static final String KEY_PREFIX = "flashsale:slots:";
    private static final Duration SLOT_TTL  = Duration.ofSeconds(86400);

    private final StringRedisTemplate redisTemplate;

    private final DefaultRedisScript<Long> reserveSlotsScript = buildScript();

    private static DefaultRedisScript<Long> buildScript() {
        var script = new DefaultRedisScript<Long>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("scripts/reserve-slots.lua")));
        script.setResultType(Long.class);
        return script;
    }

    /**
     * Atomically decrements the slot counter.
     *
     * @return remaining slots after decrement, 0 if insufficient, -1 on cache miss
     */
    @Retryable(retryFor = Exception.class, maxAttempts = 2)
    public long reserveSlots(String saleId, int quantity) {
        var key = KEY_PREFIX + saleId;
        Long result = redisTemplate.execute(reserveSlotsScript, List.of(key), String.valueOf(quantity));
        return result == null ? -1L : result;
    }

    /** Fallback when retry exhausted — returns -1 to trigger DB-based seeding path. */
    @Recover
    public long recoverReserveSlots(Exception ex, String saleId, int quantity) {
        log.warn("Redis reserve failed for saleId={}, quantity={}: {}", saleId, quantity, ex.getMessage());
        return -1L;
    }

    /** Seeds the counter with SET NX + TTL (safe on restart). */
    public void seedIfAbsent(String saleId, int remaining) {
        var key = KEY_PREFIX + saleId;
        redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(remaining), SLOT_TTL);
    }

    /** Seeds unconditionally (use only during activate). */
    public void seed(String saleId, int remaining) {
        var key = KEY_PREFIX + saleId;
        redisTemplate.opsForValue().set(key, String.valueOf(remaining), SLOT_TTL);
    }

    /** Increments the slot counter (rollback path). */
    public void incrementBy(String saleId, int quantity) {
        var key = KEY_PREFIX + saleId;
        redisTemplate.opsForValue().increment(key, quantity);
    }

    /** Reads the current counter value; returns null if key absent. */
    public Integer getRemaining(String saleId) {
        var key = KEY_PREFIX + saleId;
        var val = redisTemplate.opsForValue().get(key);
        return val == null ? null : Integer.parseInt(val);
    }
}
