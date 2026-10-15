package com.ecommerce.payment_service.entity;

public enum PaymentStatus {
    PENDING,    // we know the amount, but haven't charged yet
    COMPLETED,  // money taken
    FAILED,     // card declined
    CANCELLED   // order died before we charged: never charge it
}
