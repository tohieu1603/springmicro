package com.hieu.order_service.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates unique, human-readable order numbers via Redis INCR.
 * Format: {@code ORD-yyyyMMdd-000001} — atomic, single round-trip, no DB sequence.
 * The daily key is assigned a 48-hour TTL so it expires naturally after the day rolls over.
 */
@Service
@RequiredArgsConstructor
public class OrderNumberService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.BASIC_ISO_DATE;

    private final StringRedisTemplate stringRedisTemplate;

    public String next() {
        var today = LocalDate.now().format(FMT);
        var key = "order:seq:" + today;
        Long seq = stringRedisTemplate.opsForValue().increment(key);
        if (seq != null && seq == 1L) {
            stringRedisTemplate.expire(key, Duration.ofDays(2));
        }
        return String.format("ORD-%s-%06d", today, seq != null ? seq : 0L);
    }
}
