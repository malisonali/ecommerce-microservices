package com.ecommerce.order_service.event;

import com.ecommerce.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventListener {

    private final OrderService orderService;

    @KafkaListener(topics = "inventory-events")
    public void onInventoryEvent(InventoryEvent event,
                                 @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                 @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received {} for order {} (key={}, partition={}, offset={})",
                event.eventType(), event.orderId(), key, partition, offset);

        switch (event.eventType()) {
            case InventoryEvent.STOCK_RESERVED ->
                    orderService.confirmOrder(event.orderId());
            case InventoryEvent.STOCK_RESERVATION_FAILED ->
                    orderService.rejectOrder(event.orderId(), event.reason());
            default ->
                    log.info("Ignoring {} for order {}", event.eventType(), event.orderId());
        }
    }
}