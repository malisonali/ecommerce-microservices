package com.ecommerce.product_service.event;

import com.ecommerce.product_service.exception.InsufficientStockException;
import com.ecommerce.product_service.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventListener{
    private final InventoryService inventoryService;

    public OrderEventListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @KafkaListener(topics = "order-events")
    public void onOrderEvent(OrderPlacedEvent event,
                             @Header(KafkaHeaders.RECEIVED_KEY) String key,
                             @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                             @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received {} for order {} (key={}, partition={}, offset={}) with {} item(s)",
                event.eventType(), event.orderId(), key, partition, offset, event.items().size());

        if(!OrderPlacedEvent.TYPE.equals(event.eventType())){
            log.info("Ignoring {} for order {}", event.eventType(), event.orderId());
            return;
        }

        try {
            inventoryService.reserveStock(event);
        } catch (InsufficientStockException e) {
            log.warn("Stock reservation failed for order {}: {}", event.orderId(), e.getMessage());
        }
    }
}
