package com.hieu.cart_service.redis;

import com.hieu.cart_service.dto.CartDTO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis cache facade for cart data.
 *
 * <p>Key layout:
 * <ul>
 *   <li>{@code cart:user:{userId}} — full CartDTO, TTL 24 h</li>
 *   <li>{@code cart:idem:{key}} — idempotency result, TTL 24 h</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CartCacheService {

    private static final Logger log = LoggerFactory.getLogger(CartCacheService.class);
    private static final String CART_KEY_PREFIX = "cart:user:";
    private static final String IDEM_KEY_PREFIX = "cart:idem:";
    private static final Duration TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;

    /** Returns cached cart or null. */
    public CartDTO getCart(String userId) {
        try {
            return (CartDTO) redisTemplate.opsForValue().get(CART_KEY_PREFIX + userId);
        } catch (Exception e) {
            log.warn("Redis getCart failed for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /** Puts cart into cache. */
    public void putCart(String userId, CartDTO cart) {
        try {
            redisTemplate.opsForValue().set(CART_KEY_PREFIX + userId, cart, TTL);
        } catch (Exception e) {
            log.warn("Redis putCart failed for user {}: {}", userId, e.getMessage());
        }
    }

    /** Evicts cart cache entry. */
    public void evictCart(String userId) {
        try {
            redisTemplate.delete(CART_KEY_PREFIX + userId);
        } catch (Exception e) {
            log.warn("Redis evictCart failed for user {}: {}", userId, e.getMessage());
        }
    }

    /** Gets cached idempotency result or null. */
    public CartDTO getIdempotentResult(String key) {
        try {
            return (CartDTO) redisTemplate.opsForValue().get(IDEM_KEY_PREFIX + key);
        } catch (Exception e) {
            log.warn("Redis getIdempotentResult failed key={}: {}", key, e.getMessage());
            return null;
        }
    }

    /** Stores idempotency result. */
    public void putIdempotentResult(String key, CartDTO cart) {
        try {
            redisTemplate.opsForValue().set(IDEM_KEY_PREFIX + key, cart, TTL);
        } catch (Exception e) {
            log.warn("Redis putIdempotentResult failed key={}: {}", key, e.getMessage());
        }
    }
}
