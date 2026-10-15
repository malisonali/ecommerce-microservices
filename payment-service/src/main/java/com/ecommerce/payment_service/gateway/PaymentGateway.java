package com.ecommerce.payment_service.gateway;

import java.math.BigDecimal;

public interface PaymentGateway {

    ChargeResult charge(Long orderId, BigDecimal amount);

    record ChargeResult(boolean success, String transactionId, String failureReason) {

        public static ChargeResult approved(String transactionId) {
            return new ChargeResult(true, transactionId, null);
        }

        public static ChargeResult declined(String reason) {
            return new ChargeResult(false, null, reason);
        }
    }
}