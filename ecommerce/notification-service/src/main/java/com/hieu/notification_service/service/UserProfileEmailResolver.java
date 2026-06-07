package com.hieu.notification_service.service;

import com.hieu.user_profile_service.grpc.GetEmailRequest;
import com.hieu.user_profile_service.grpc.GetEmailResponse;
import com.hieu.user_profile_service.grpc.UserProfileServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Resolves a user's email by reading the local Redis cache populated by the
 * {@code user.profile-upserted} Kafka consumer (Event-Carried State Transfer).
 *
 * <p>If the cache misses (e.g. consumer hasn't caught up yet, profile created
 * before this service was deployed) the resolver falls back to a synchronous
 * gRPC lookup against user-profile-service, then warms the cache.
 *
 * <p>Cache TTL is generous (24h) because profile email changes are rare and the
 * Kafka consumer overwrites the entry anyway when an upsert event arrives.
 */
@Service
@Slf4j
public class UserProfileEmailResolver {

    static final String KEY_PREFIX = "userprofile:email:";
    static final Duration CACHE_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;
    private final UserProfileServiceGrpc.UserProfileServiceBlockingStub stub;

    public UserProfileEmailResolver(StringRedisTemplate redis, GrpcChannelFactory channelFactory) {
        this.redis = redis;
        var channel = channelFactory.createChannel("user-profile-service");
        this.stub = UserProfileServiceGrpc.newBlockingStub(channel);
    }

    public Optional<String> lookupEmail(String userId) {
        if (userId == null || userId.isBlank()) return Optional.empty();

        String cached = safeGet(redis, KEY_PREFIX + userId);
        if (cached != null && !cached.isBlank()) return Optional.of(cached);

        // Cache miss → gRPC fallback. Worth the extra hop only the first time
        // we hear about a user. Bounded deadline so a hung user-profile-service
        // can't pin mailExecutor threads indefinitely.
        try {
            GetEmailResponse response = stub
                    .withDeadlineAfter(1500, TimeUnit.MILLISECONDS)
                    .getEmail(GetEmailRequest.newBuilder().setUserId(userId).build());
            if (response.getFound() && !response.getEmail().isBlank()) {
                cache(userId, response.getEmail());
                return Optional.of(response.getEmail());
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("gRPC email fallback failed for userId={}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    /** Called by the Kafka consumer to refresh on every {@code user.profile-upserted} event. */
    public void cache(String userId, String email) {
        if (userId == null || userId.isBlank() || email == null || email.isBlank()) return;
        try {
            redis.opsForValue().set(KEY_PREFIX + userId, email, CACHE_TTL);
        } catch (Exception e) {
            // Redis being down should never block notification delivery —
            // gRPC fallback is still available next time.
            log.warn("Failed to cache email for userId={}: {}", userId, e.getMessage());
        }
    }

    private static String safeGet(StringRedisTemplate redis, String key) {
        try {
            return redis.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis GET failed for key={}: {}", key, e.getMessage());
            return null;
        }
    }
}
