package com.hieu.order_service;

import com.hieu.order_service.application.dto.OrderDTO;
import com.hieu.order_service.application.service.IdempotencyService;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis Lua idempotency tests — Redis-only, no Postgres or Kafka needed.
 */
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.grpc.server.port=0",
        "outbox.poller.enabled=false",
        // Use in-memory H2 for JPA (idempotency DB record) — avoids Postgres container
        "spring.datasource.url=jdbc:h2:mem:idemtestdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.kafka.bootstrap-servers=localhost:9999",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
class IdempotencyServiceRedisLuaTest {

    @Container
    static final RedisContainer redis =
            new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    IdempotencyService idempotencyService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    private static final String REDIS_PREFIX = "order:idem:";

    @BeforeEach
    void flushRedis() {
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void shouldClaimIdempotencyKeyAtomically() throws InterruptedException {
        String userId = "user-atomic-" + System.nanoTime();
        String key = "key-atomic-" + System.nanoTime();

        int threads = 8;
        AtomicInteger newCount = new AtomicInteger(0);
        AtomicInteger replayCount = new AtomicInteger(0);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    try {
                        Optional<OrderDTO> result = idempotencyService.checkOrCreate(userId, key);
                        if (result.isEmpty()) {
                            newCount.incrementAndGet();
                        } else {
                            replayCount.incrementAndGet();
                        }
                    } catch (Exception ignored) {
                        // DuplicateOrderException counts as "not new"
                        replayCount.incrementAndGet();
                    }
                }));
            }

            ready.await(5, TimeUnit.SECONDS);
            start.countDown(); // all threads race together

            for (var f : futures) {
                try { f.get(10, TimeUnit.SECONDS); } catch (Exception ignored) {}
            }
        }

        // Exactly one thread must see "new" — the Lua SETNX ensures atomicity
        assertThat(newCount.get()).isEqualTo(1);
        assertThat(replayCount.get()).isEqualTo(threads - 1);
    }

    @Test
    void shouldReturnCachedDTOOnReplay() throws Exception {
        String userId = "user-cache-" + System.nanoTime();
        String key = "key-cache-" + System.nanoTime();
        String redisKey = REDIS_PREFIX + userId + ":" + key;

        // Simulate a completed order already cached in Redis
        String cachedJson = """
                {"id":42,"orderNumber":"ORD-20260424-000001","userId":"%s",
                 "status":"PENDING","items":[],"totalAmount":100.00,
                 "recipientName":"Test","recipientPhone":"0987654321",
                 "paymentMethod":"COD","createdAt":"2026-04-24T00:00:00Z",
                 "updatedAt":"2026-04-24T00:00:00Z"}
                """.formatted(userId);

        // Put the JSON directly (simulating markCompleted having run)
        stringRedisTemplate.opsForValue().set(redisKey, cachedJson.strip());

        // checkOrCreate should detect the existing value and return the cached DTO
        Optional<OrderDTO> result = idempotencyService.checkOrCreate(userId, key);
        // The Lua script returns the existing value (the JSON) → service parses it as DTO
        // OR returns the cached body — either way, result must be non-empty
        assertThat(result).isPresent();
        assertThat(result.get().userId()).isEqualTo(userId);
    }

    @Test
    void shouldExpireAfterTTL() throws Exception {
        String userId = "user-ttl-" + System.nanoTime();
        String key = "key-ttl-" + System.nanoTime();
        String redisKey = REDIS_PREFIX + userId + ":" + key;

        // Set a very short TTL directly
        stringRedisTemplate.opsForValue().set(redisKey, "PROCESSING",
                java.time.Duration.ofMillis(200));

        // Key exists now
        assertThat(stringRedisTemplate.hasKey(redisKey)).isTrue();

        // Wait for expiry
        Thread.sleep(400);

        // Key must be gone
        assertThat(stringRedisTemplate.hasKey(redisKey)).isFalse();
    }
}
