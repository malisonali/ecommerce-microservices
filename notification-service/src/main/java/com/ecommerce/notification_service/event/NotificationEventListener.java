package com.ecommerce.notification_service.event;

import com.ecommerce.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = "order-events",
            properties = "spring.json.value.default.type=com.ecommerce.notification_service.event.OrderEvent")
    public void onOrderEvent(OrderEvent event) {
        log.info("Received {} for order {}", event.eventType(), event.orderId());
        notificationService.handleOrderEvent(event);
    }

    @KafkaListener(topics = "inventory-events",
            properties = "spring.json.value.default.type=com.ecommerce.notification_service.event.InventoryEvent")
    public void onInventoryEvent(InventoryEvent event) {
        log.info("Received {} for order {}", event.eventType(), event.orderId());
        notificationService.handleInventoryEvent(event);
    }
}