package com.ecommerce.payment_service.event;

import com.ecommerce.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventListener {

    private final PaymentService paymentService;
    private final PaymentEventPublisher publisher;

    @KafkaListener(topics = "inventory-events",
            properties = "spring.json.value.default.type=com.ecommerce.payment_service.event.InventoryEvent")
    public void onInventoryEvent(InventoryEvent event) {
        log.info("Received {} for order {}", event.eventType(), event.orderId());

        // 1. Decide and save (transaction commits when this returns)
        // 2. Then tell everyone
        paymentService.handleInventoryEvent(event).ifPresent(publisher::publish);
    }
}