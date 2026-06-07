package com.hieu.user_profile_service.exception;

public class AddressNotFoundException extends RuntimeException {
    public AddressNotFoundException(String addressId) {
        super("Address not found: " + addressId);
    }
}
