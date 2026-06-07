package com.hieu.order_service.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Polls the {@code outbox_events} table and publishes pending events to Kafka.
 * {@code FOR UPDATE SKIP LOCKED} lets multiple instances share the workload safely.
 *
 * <p>Retry policy: exponential backoff (2s, 4s, 8s, 16s... capped at 5m) up to
 * {@link #MAX_RETRIES}. After max retries the row is shipped to a dead-letter topic
 * ({@code order.outbox.dlt}) so operators can inspect + manually replay, then marked
 * as processed so the poller doesn't keep churning on it.
 *
 * <p>Disabled via {@code outbox.poller.enabled=false} in tests that don't want the
 * scheduled side-effects interfering with assertions.
 */
@Component
@ConditionalOnProperty(name = "outbox.poller.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);
    private static final int MAX_RETRIES = 10;
    private static final long BACKOFF_BASE_SECONDS = 2L;
    private static final long BACKOFF_CAP_SECONDS = 300L; // 5 minutes
    /**
     * Per-send timeout. Bounds the worst-case DB connection hold inside the @Transactional
     * poll loop: with batchSize=100, total hold ≤ 100 × this. Without a timeout, a stalled
     * Kafka broker can pin every connection in the HikariCP pool indefinitely.
     */
    private static final long SEND_TIMEOUT_SECONDS = 5L;
    /** Dead-letter topic — operators replay from here via a CLI script. */
    public static final String DLT_TOPIC = "order.outbox.dlt";

    @Value("${outbox.batch-size:100}")
    private int batchSize;

    private final OutboxEventJpaRepository repository;
    private final KafkaTemplate<String, String> stringKafkaTemplate;

    @Scheduled(fixedDelayString = "${outbox.poll-delay-ms:500}")
    @Transactional
    public void poll() {
        var batch = repository.lockBatch(batchSize);
        if (batch.isEmpty()) return;

        for (var event : batch) {
            if (event.getRetryCount() >= MAX_RETRIES) {
                // Park in DLT + stop retrying so the poller moves on to healthy rows.
                shipToDeadLetter(event);
                continue;
            }
            try {
                stringKafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                        .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                repository.markProcessed(event.getId(), Instant.now());
                log.debug("Outbox published id={} type={} topic={}",
                        event.getId(), event.getEventType(), event.getTopic());
            } catch (TimeoutException te) {
                int newRetryCount = event.getRetryCount() + 1;
                Instant nextAttempt = Instant.now().plusSeconds(backoffSeconds(newRetryCount));
                repository.bumpRetry(event.getId(), newRetryCount, nextAttempt);
                log.warn("Outbox publish timed out id={} after {}s retry={}",
                        event.getId(), SEND_TIMEOUT_SECONDS, newRetryCount);
            } catch (Exception e) {
                int newRetryCount = event.getRetryCount() + 1;
                Instant nextAttempt = Instant.now().plusSeconds(backoffSeconds(newRetryCount));
                repository.bumpRetry(event.getId(), newRetryCount, nextAttempt);
                log.warn("Outbox publish failed id={} type={} retry={} nextAttemptAt={}: {}",
                        event.getId(), event.getEventType(), newRetryCount, nextAttempt, e.getMessage());
            }
        }
    }

    private void shipToDeadLetter(OutboxEventJpaEntity event) {
        try {
            // Prefix the key so DLT consumers can tell which topic the message belongs to
            // without needing to parse the payload.
            String dltKey = event.getTopic() + ":" + event.getAggregateId();
            stringKafkaTemplate.send(DLT_TOPIC, dltKey, event.getPayload())
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            repository.markProcessed(event.getId(), Instant.now());
            log.error("Outbox event id={} type={} DLT'd after {} retries — operator action required",
                    event.getId(), event.getEventType(), event.getRetryCount());
        } catch (Exception e) {
            // If DLT publish also fails, don't advance the row — try again next tick.
            log.error("Outbox DLT publish failed for id={}; will retry: {}", event.getId(), e.getMessage());
        }
    }

    private long backoffSeconds(int retryCount) {
        // 2s, 4s, 8s, 16s, ... capped at 300s
        long seconds = BACKOFF_BASE_SECONDS << Math.min(retryCount - 1, 7);
        return Math.min(seconds, BACKOFF_CAP_SECONDS);
    }
}
