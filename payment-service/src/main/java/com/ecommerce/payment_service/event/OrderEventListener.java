package com.ecommerce.payment_service.event;

import com.ecommerce.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final PaymentService paymentService;

    @KafkaListener(topics = "order-events",
            properties = "spring.json.value.default.type=com.ecommerce.payment_service.event.OrderEvent")
    public void onOrderEvent(OrderEvent event) {
        log.info("Received {} for order {}", event.eventType(), event.orderId());
        paymentService.handleOrderEvent(event);
    }
}