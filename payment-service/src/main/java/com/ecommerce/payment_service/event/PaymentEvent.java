package com.ecommerce.payment_service.event;

import com.ecommerce.payment_service.entity.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Long orderId,
        String userEmail,
        BigDecimal amount,
        String transactionId,
        String reason
) {
    public static final String PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";

    public static PaymentEvent completed(Payment payment) {
        return new PaymentEvent(UUID.randomUUID().toString(), PAYMENT_COMPLETED, Instant.now(),
                payment.getOrderId(), payment.getUserEmail(), payment.getAmount(),
                payment.getTransactionId(), null);
    }

    public static PaymentEvent failed(Payment payment) {
        return new PaymentEvent(UUID.randomUUID().toString(), PAYMENT_FAILED, Instant.now(),
                payment.getOrderId(), payment.getUserEmail(), payment.getAmount(),
                null, payment.getFailureReason());
    }
}
