package com.hieu.user_profile_service.exception;

public class UserProfileNotFoundException extends RuntimeException {
    public UserProfileNotFoundException(String userId) {
        super("User profile not found: " + userId);
    }
}
