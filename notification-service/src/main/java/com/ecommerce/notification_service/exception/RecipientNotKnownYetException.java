package com.ecommerce.notification_service.exception;

public class RecipientNotKnownYetException extends RuntimeException {

    public RecipientNotKnownYetException(Long orderId) {
        super("No email known yet for order " + orderId);
    }
}