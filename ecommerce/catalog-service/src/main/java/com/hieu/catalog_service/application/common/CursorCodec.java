package com.hieu.catalog_service.application.common;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/**
 * Opaque cursor codec for keyset pagination.
 *
 * <p>Encodes {@code (createdAt, id)} as a URL-safe base64 string. The id is a UUID
 * {@code String} in catalog-service; epoch micros preserve ordering even when rows are
 * created in the same millisecond (Postgres stores micros). Malformed input throws
 * {@link IllegalArgumentException} so clients cannot forge cursors that skip ahead.
 */
public final class CursorCodec {

    private static final String DELIM = "|";

    private CursorCodec() {}

    public record Cursor(Instant createdAt, String id) {
        public Cursor {
            Objects.requireNonNull(createdAt, "createdAt");
            Objects.requireNonNull(id, "id");
        }
    }

    public static String encode(Instant createdAt, String id) {
        if (createdAt == null || id == null) return null;
        long epochMicros = createdAt.getEpochSecond() * 1_000_000L + createdAt.getNano() / 1_000;
        String raw = epochMicros + DELIM + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Cursor decode(String opaque) {
        if (opaque == null || opaque.isBlank()) return null;
        try {
            String raw = new String(Base64.getUrlDecoder().decode(opaque), StandardCharsets.UTF_8);
            int split = raw.indexOf(DELIM);
            if (split < 0) throw new IllegalArgumentException("missing delimiter");

            long epochMicros = Long.parseLong(raw.substring(0, split));
            String id = raw.substring(split + DELIM.length());
            Instant ts = Instant.ofEpochSecond(
                    epochMicros / 1_000_000L,
                    (epochMicros % 1_000_000L) * 1_000L);
            return new Cursor(ts, id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid cursor: " + e.getMessage(), e);
        }
    }
}
