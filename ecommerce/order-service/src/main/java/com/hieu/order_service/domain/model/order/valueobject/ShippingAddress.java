package com.hieu.order_service.domain.model.order.valueobject;

import java.util.Objects;
import java.util.regex.Pattern;

/** Full shipping address. Country defaults to Vietnam. */
public record ShippingAddress(
        String street,
        String ward,
        String district,
        String city,
        String country,
        String postalCode) {

    private static final Pattern VN_POSTAL = Pattern.compile("^\\d{5,6}$");

    public ShippingAddress {
        Objects.requireNonNull(street,   "street");
        Objects.requireNonNull(ward,     "ward");
        Objects.requireNonNull(district, "district");
        Objects.requireNonNull(city,     "city");
        if (street.isBlank())   throw new IllegalArgumentException("street must not be blank");
        if (ward.isBlank())     throw new IllegalArgumentException("ward must not be blank");
        if (district.isBlank()) throw new IllegalArgumentException("district must not be blank");
        if (city.isBlank())     throw new IllegalArgumentException("city must not be blank");
        street   = street.trim();
        ward     = ward.trim();
        district = district.trim();
        city     = city.trim();
        country  = (country == null || country.isBlank()) ? "Vietnam" : country.trim();
        // postalCode is optional; if provided, must be 5–6 digits (Vietnam format).
        if (postalCode != null && !postalCode.isBlank()) {
            postalCode = postalCode.trim();
            if (!VN_POSTAL.matcher(postalCode).matches()) {
                throw new IllegalArgumentException("postalCode must be 5–6 digits, got: " + postalCode);
            }
        } else {
            postalCode = null;
        }
    }

    public static ShippingAddress of(String street, String ward, String district, String city,
                                     String country, String postalCode) {
        return new ShippingAddress(street, ward, district, city, country, postalCode);
    }
}
