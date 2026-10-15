package com.ecommerce.payment_service.exception;

public class PaymentNotKnownYetException extends RuntimeException {

    public PaymentNotKnownYetException(Long orderId) {
        super("No payment known yet for order " + orderId);
    }
}