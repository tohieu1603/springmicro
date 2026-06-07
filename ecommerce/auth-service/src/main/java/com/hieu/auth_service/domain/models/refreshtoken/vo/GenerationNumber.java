package com.hieu.auth_service.domain.models.refreshtoken.vo;

/**
 * Value Object: monotonically increasing rotation depth with a token family
 *
 * Root token starts at 0. Each successful rotation increments by 1
 * A gap or replay of an older generation signals a potential token-theft attack
 */
public record GenerationNumber(int value) {

    // Compact Constructor để validate
    public GenerationNumber {
        if (value < 0) {
            throw new IllegalArgumentException("Generation must be >= 0, got " + value);
        }
    }

    public static GenerationNumber of(int value) {
        return new GenerationNumber(value);
    }

    /**
     * Generation 0 - the first token issued for a new family (after login).
     */
    public static GenerationNumber root() {
        return new GenerationNumber(0);
    }

    public boolean isRoot() {
        return value == 0;
    }

    public GenerationNumber next() {
        return new GenerationNumber(value + 1);
    }

    // Comparison helpers (useful in reuse-detection policies)
    public boolean isAfter(GenerationNumber other) {
        return this.value > other.value();
    }

    public boolean isBefore(GenerationNumber other) {
        return this.value < other.value();
    }

    public boolean isSameAs(GenerationNumber other) {
        return this.value == other.value();
    }

    /**
     * Signed distance: positive means this is further along the chain.
     */
    public int difference(GenerationNumber other) {
        return this.value - other.value();
    }
}