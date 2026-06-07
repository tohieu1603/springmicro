package com.hieu.cart_service.exception;

/** Raised when a cart item cannot be found for a given user+variant combination. */
public class CartItemNotFoundException extends RuntimeException {

    public CartItemNotFoundException(String userId, String variantId) {
        super("Cart item not found for user=%s variant=%s".formatted(userId, variantId));
    }
}
