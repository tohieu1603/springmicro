package com.hieu.order_service.application.handler.order;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Generates unique order numbers: {@code ORD-yyyyMMdd-NNNNNN} using Redis INCR per day. */
@Component
@RequiredArgsConstructor
public class OrderNumberGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final StringRedisTemplate redis;

    public String generate() {
        var date = LocalDate.now().format(FMT);
        var key = "order:seq:" + date;
        var seq = redis.opsForValue().increment(key);
        if (seq == 1L) {
            // Set expiry only on first creation (86400s + 1h buffer)
            redis.expire(key, java.time.Duration.ofSeconds(90_000));
        }
        return "ORD-" + date + "-" + String.format("%06d", seq);
    }
}
