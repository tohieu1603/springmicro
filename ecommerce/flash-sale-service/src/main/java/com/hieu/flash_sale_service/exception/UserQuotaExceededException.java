package com.hieu.flash_sale_service.exception;

/** Thrown when a user has already claimed their maximum allowed quantity for a flash sale. */
public class UserQuotaExceededException extends RuntimeException {

    public UserQuotaExceededException(String userId, String saleId, int maxPerUser) {
        super("User " + userId + " has reached max quota (" + maxPerUser + ") for sale id=" + saleId);
    }
}
