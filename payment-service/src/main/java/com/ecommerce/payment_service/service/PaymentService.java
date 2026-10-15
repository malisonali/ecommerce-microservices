package com.ecommerce.payment_service.service;

import com.ecommerce.payment_service.entity.Payment;
import com.ecommerce.payment_service.event.InventoryEvent;
import com.ecommerce.payment_service.event.OrderEvent;
import com.ecommerce.payment_service.event.PaymentEvent;
import com.ecommerce.payment_service.exception.PaymentNotKnownYetException;
import com.ecommerce.payment_service.gateway.PaymentGateway;
import com.ecommerce.payment_service.gateway.PaymentGateway.ChargeResult;
import com.ecommerce.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;

    // ---------- Notes from the front desk ----------

    @Transactional
    public void handleOrderEvent(OrderEvent event) {
        Long orderId = event.orderId();

        switch (event.eventType()) {
            case OrderEvent.ORDER_PLACED -> {
                if (paymentRepository.existsById(orderId)) {
                    log.info("Payment for order {} already recorded, skipping duplicate", orderId);
                    return;
                }
                paymentRepository.save(Payment.pending(orderId, event.userEmail(), event.totalAmount()));
                log.info("Order {} owes ₹{}: recorded as PENDING (not charged yet)",
                        orderId, event.totalAmount().toPlainString());
            }

            case OrderEvent.ORDER_CANCELLED -> {
                Optional<Payment> existing = paymentRepository.findById(orderId);

                if (existing.isEmpty()) {
                    // The cancel arrived first: leave a marker so we never charge later
                    paymentRepository.save(Payment.cancelledBeforeKnown(orderId, event.userEmail()));
                    log.info("Order {} cancelled before we saw it: saved CANCELLED marker", orderId);
                    return;
                }

                Payment payment = existing.get();
                switch (payment.getStatus()) {
                    case PENDING -> {
                        payment.cancel();
                        log.info("Order {} cancelled before charging: payment CANCELLED", orderId);
                    }
                    case COMPLETED -> log.warn(
                            "Order {} cancelled AFTER payment {}: refund needed (Day 20)",
                            orderId, payment.getTransactionId());
                    case FAILED, CANCELLED -> log.info(
                            "Order {} cancelled, payment already {}: nothing to do",
                            orderId, payment.getStatus());
                }
            }

            default -> log.debug("Ignoring {} for order {}", event.eventType(), orderId);
        }
    }

    // ---------- Replies from the storeroom ----------

    /**
     * Returns the result to publish, or empty if there's nothing to say.
     * The caller publishes it AFTER this transaction has committed.
     */
    @Transactional
    public Optional<PaymentEvent> handleInventoryEvent(InventoryEvent event) {
        Long orderId = event.orderId();

        Payment payment = paymentRepository.findById(orderId)
                .orElseThrow(() -> new PaymentNotKnownYetException(orderId));

        switch (event.eventType()) {
            case InventoryEvent.STOCK_RESERVED -> {
                return Optional.ofNullable(chargeOnce(payment));
            }

            case InventoryEvent.STOCK_RESERVATION_FAILED -> {
                if (payment.isPending()) {
                    payment.cancel();
                    log.info("No stock for order {}: payment CANCELLED, customer not charged", orderId);
                }
                return Optional.empty();
            }

            default -> {
                log.debug("Ignoring {} for order {}", event.eventType(), orderId);
                return Optional.empty();
            }
        }
    }

    private PaymentEvent chargeOnce(Payment payment) {
        Long orderId = payment.getOrderId();

        return switch (payment.getStatus()) {
            case PENDING -> {
                ChargeResult result = paymentGateway.charge(orderId, payment.getAmount());
                if (result.success()) {
                    payment.complete(result.transactionId());
                    yield PaymentEvent.completed(payment);
                } else {
                    payment.fail(result.failureReason());
                    yield PaymentEvent.failed(payment);
                }
            }
            // Decide once, reply every time (the Day 14 lesson)
            case COMPLETED -> {
                log.info("Order {} already paid ({}): re-sending result, NOT charging again",
                        orderId, payment.getTransactionId());
                yield PaymentEvent.completed(payment);
            }
            case FAILED -> {
                log.info("Order {} payment already failed: re-sending result", orderId);
                yield PaymentEvent.failed(payment);
            }
            case CANCELLED -> {
                log.info("Order {} was cancelled before payment: not charging", orderId);
                yield null;
            }
        };
    }
}