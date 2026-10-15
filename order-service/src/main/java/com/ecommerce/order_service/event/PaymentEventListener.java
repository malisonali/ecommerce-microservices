package com.ecommerce.order_service.event;

import com.ecommerce.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final OrderService orderService;

    @KafkaListener(topics = "payment-events",
            properties = "spring.json.value.default.type=com.ecommerce.order_service.event.PaymentEvent")
    public void onPaymentEvent(PaymentEvent event) {
        log.info("Received {} for order {}", event.eventType(), event.orderId());

        switch (event.eventType()) {
            case PaymentEvent.PAYMENT_COMPLETED ->
                    orderService.confirmPaidOrder(event.orderId(), event.transactionId());
            case PaymentEvent.PAYMENT_FAILED ->
                    orderService.cancelUnpaidOrder(event.orderId(), event.reason());
            default ->
                    log.info("Ignoring {} for order {}", event.eventType(), event.orderId());
        }
    }
}