package com.hieu.catalog_service.domain.model.product.valueobject;

import java.text.Normalizer;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * URL-friendly identifier derived from the product name. Slugs are unique per tenant
 * and must survive re-encoding by HTTP intermediaries, so we keep them strictly
 * {@code [a-z0-9-]}.
 *
 * <p>{@link #generate(String)} is best-effort and may collide — the infrastructure layer
 * is expected to append a disambiguating suffix when the DB's unique constraint fires.
 */
public record Slug(String value) {

    private static final Pattern ALLOWED = Pattern.compile("^[a-z0-9][a-z0-9-]{0,127}$");
    private static final Pattern NON_ALPHANUM = Pattern.compile("[^a-z0-9\\s-]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern MULTI_DASH = Pattern.compile("-{2,}");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    public Slug {
        Objects.requireNonNull(value, "Slug cannot be null");
        if (!ALLOWED.matcher(value).matches()) {
            throw new IllegalArgumentException(
                "Slug must be 1-128 chars, lowercase alphanumeric with '-': " + value);
        }
    }

    public static Slug of(String value) {
        return new Slug(value);
    }

    /**
     * Normalises {@code name} into a URL-safe slug: strips diacritics, lowercases,
     * collapses whitespace into a single dash, and trims leading/trailing dashes.
     * Collision handling is the persistence layer's job.
     */
    public static Slug generate(String name) {
        Objects.requireNonNull(name, "name is required to generate a slug");
        String normalised = Normalizer.normalize(name, Normalizer.Form.NFD);
        normalised = DIACRITICS.matcher(normalised).replaceAll("");
        normalised = normalised.toLowerCase();
        normalised = NON_ALPHANUM.matcher(normalised).replaceAll(" ");
        normalised = WHITESPACE.matcher(normalised.trim()).replaceAll("-");
        normalised = MULTI_DASH.matcher(normalised).replaceAll("-");
        normalised = trimDashes(normalised);
        if (normalised.isBlank()) {
            throw new IllegalArgumentException("Name '" + name + "' produced an empty slug");
        }
        if (normalised.length() > 128) {
            normalised = normalised.substring(0, 128);
            normalised = trimDashes(normalised);
            if (normalised.isBlank()) {
                throw new IllegalArgumentException("Name '" + name + "' produced an empty slug after truncation");
            }
        }
        return new Slug(normalised);
    }

    private static String trimDashes(String s) {
        int start = 0, end = s.length();
        while (start < end && s.charAt(start) == '-') start++;
        while (end > start && s.charAt(end - 1) == '-') end--;
        return s.substring(start, end);
    }
}
