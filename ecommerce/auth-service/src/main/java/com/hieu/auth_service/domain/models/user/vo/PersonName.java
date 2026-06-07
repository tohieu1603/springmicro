package com.hieu.auth_service.domain.models.user.vo;


/**
 * Value Object representing a Person's Full Name.
 * Encapsulates first name and last name together.
 */
public record PersonName(String firstName, String lastName) {

    // ── Compact Constructor ──
    public PersonName {
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be null or empty");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be null or empty");
        }

        // Tự động trim khoảng trắng thừa
        firstName = firstName.trim();
        lastName = lastName.trim();
    }

    public static PersonName of(String firstName, String lastName) {
        return new PersonName(firstName, lastName);
    }

    // Đổi getFullName -> fullName
    public String fullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return fullName();
    }
}