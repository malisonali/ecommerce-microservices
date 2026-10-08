package com.ecommerce.product_service.event;

import com.ecommerce.product_service.entity.ReservationStatus;
import com.ecommerce.product_service.entity.StockReservation;
import com.ecommerce.product_service.exception.InsufficientStockException;
import com.ecommerce.product_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final InventoryService inventoryService;
    private final InventoryEventPublisher inventoryEventPublisher;

    @KafkaListener(topics = "order-events")
    public void onOrderEvent(OrderPlacedEvent event,
                             @Header(KafkaHeaders.RECEIVED_KEY) String key,
                             @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                             @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received {} for order {} (key={}, partition={}, offset={}) with {} item(s)",
                event.eventType(), event.orderId(), key, partition, offset, event.items().size());

        if (!OrderPlacedEvent.TYPE.equals(event.eventType())) {
            log.info("Ignoring {} for order {}", event.eventType(), event.orderId());
            return;
        }

        // 1. Decide (only happens once per order)
        try {
            boolean reserved = inventoryService.reserveStock(event);
            if (!reserved) {
                log.info("Duplicate event for order {}, re-sending the earlier decision", event.orderId());
            }
        } catch (InsufficientStockException e) {
            boolean recorded = inventoryService.recordRejection(event, e.getMessage());
            log.warn("Stock reservation failed for order {}: {} (recorded={})",
                    event.orderId(), e.getMessage(), recorded);
        }

        // 2. Reply with whatever was decided (safe to send more than once)
        StockReservation reservation = inventoryService.getReservation(event.orderId());

        InventoryEvent reply = reservation.getStatus() == ReservationStatus.RESERVED
                ? InventoryEvent.reserved(event.orderId())
                : InventoryEvent.failed(event.orderId(), reservation.getReason());

        inventoryEventPublisher.publish(reply);
    }
}