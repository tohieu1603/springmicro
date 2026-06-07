package com.hieu.catalog_service.domain.model.product.valueobject;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stock-Keeping Unit — uniquely identifies a sellable variant across the catalog.
 *
 * <p>Normalised to upper-case and stripped; only alphanumerics, {@code -} and {@code _}
 * are accepted so the value stays safe inside URLs, logs, and CSV exports.
 */
public record Sku(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[A-Z0-9][A-Z0-9_-]{1,63}$");

    public Sku {
        Objects.requireNonNull(value, "SKU cannot be null");
        value = value.trim().toUpperCase();
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                "SKU must be 2-64 chars, alphanumeric plus '-' '_', starting with letter/digit: " + value);
        }
    }

    public static Sku of(String value) {
        return new Sku(value);
    }
}
