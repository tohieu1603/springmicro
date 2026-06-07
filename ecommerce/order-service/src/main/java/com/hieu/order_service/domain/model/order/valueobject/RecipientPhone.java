package com.hieu.order_service.domain.model.order.valueobject;

import java.util.Objects;
import java.util.regex.Pattern;

/** Vietnam mobile/landline phone — 10–11 digits, leading 0. */
public record RecipientPhone(String value) {

    private static final Pattern VN_PHONE = Pattern.compile("^0[1-9]\\d{8,9}$");

    public RecipientPhone {
        Objects.requireNonNull(value, "RecipientPhone");
        if (value.isBlank()) throw new IllegalArgumentException("RecipientPhone must not be blank");
        value = value.trim().replaceAll("[\\s\\-]", "");
        if (!VN_PHONE.matcher(value).matches()) {
            throw new IllegalArgumentException(
                "RecipientPhone must be a valid Vietnam phone (10–11 digits, leading 0): " + value);
        }
    }

    public static RecipientPhone of(String value) { return new RecipientPhone(value); }
}
