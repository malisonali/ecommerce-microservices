package com.ecommerce.payment_service.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
public class MockPaymentGateway implements PaymentGateway {

    private final BigDecimal maxAmount;

    public MockPaymentGateway(@Value("${payment.mock.max-amount}") BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    @Override
    public ChargeResult charge(Long orderId, BigDecimal amount) {
        if (amount.compareTo(maxAmount) > 0) {
            log.warn("MOCK GATEWAY: declined ₹{} for order {} (limit ₹{})",
                    amount.toPlainString(), orderId, maxAmount.toPlainString());
            return ChargeResult.declined("Card limit exceeded");
        }

        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("MOCK GATEWAY: charged ₹{} for order {} → {}",
                amount.toPlainString(), orderId, transactionId);
        return ChargeResult.approved(transactionId);
    }
}