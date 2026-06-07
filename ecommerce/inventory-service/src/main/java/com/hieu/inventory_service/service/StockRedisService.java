package com.hieu.inventory_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Redis operations for stock counters.
 * Keys use prefix {@code inventory:stock:{productId}}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockRedisService {

    private static final String KEY_PREFIX = "inventory:stock:";
    private static final Duration TTL = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> reserveStockScript;
    private final DefaultRedisScript<Long> releaseStockBatchScript;

    /**
     * Atomically checks and decrements stock via Lua.
     * @return 1=success, 0=insufficient, -1=cache miss
     */
    public int reserveStockAtomically(Map<String, Integer> items) {
        var keys = new ArrayList<String>();
        var args = new ArrayList<String>();
        for (var entry : items.entrySet()) {
            keys.add(KEY_PREFIX + entry.getKey());
            args.add(String.valueOf(entry.getValue()));
        }
        Long result = redisTemplate.execute(reserveStockScript, keys, args.toArray(new String[0]));
        return result != null ? result.intValue() : 0;
    }

    /** Seeds or refreshes a stock counter with a 1-hour TTL. */
    public void setStock(String productId, int quantity) {
        redisTemplate.opsForValue().set(KEY_PREFIX + productId, String.valueOf(quantity), TTL);
    }

    /**
     * Seeds the counter only if absent — avoids overwriting concurrent in-flight
     * deductions when seeding from DB after a cache miss. If the key already exists,
     * another thread has already (re)seeded it; we trust the live value.
     */
    public void setStockIfAbsent(String productId, int quantity) {
        redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + productId, String.valueOf(quantity), TTL);
    }

    /** Drops the cached counter so the next reserve re-seeds from DB under lock. */
    public void invalidate(String productId) {
        redisTemplate.delete(KEY_PREFIX + productId);
    }

    /**
     * Atomically restores stock for multiple products in a single Lua call — avoids
     * partial-failure inconsistency that per-product loop rollback cannot guarantee.
     */
    public void releaseStockBatch(Map<String, Integer> productToQty) {
        if (productToQty.isEmpty()) return;
        List<String> keys = new ArrayList<>();
        List<String> args = new ArrayList<>();
        productToQty.forEach((pid, qty) -> {
            keys.add(KEY_PREFIX + pid);
            args.add(String.valueOf(qty));
        });
        redisTemplate.execute(releaseStockBatchScript, keys, args.toArray(new String[0]));
    }

    /**
     * Restores stock (rollback or release).
     * Uses a Lua script so we only increment if the key already exists — prevents
     * creating a phantom key with no TTL when the cache has already expired, which
     * would cause Redis to permanently drift above the DB value.
     */
    public void releaseStock(String productId, int quantity) {
        String key = KEY_PREFIX + productId;
        // Conditionally INCRBY + EXPIRE only when the key exists; if missing, the next
        // setStock() call (triggered by a DB read) will seed a fresh value.
        String script = """
                local v = redis.call('GET', KEYS[1])
                if v then
                  redis.call('INCRBY', KEYS[1], ARGV[1])
                  redis.call('EXPIRE', KEYS[1], 3600)
                end
                return 0
                """;
        redisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            List.of(key),
            String.valueOf(quantity)
        );
    }
}
