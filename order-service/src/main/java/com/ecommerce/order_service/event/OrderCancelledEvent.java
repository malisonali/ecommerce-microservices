package com.ecommerce.order_service.event;

import com.ecommerce.order_service.entity.Order;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCancelledEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Long orderId,
        String userEmail,
        String cancelledBy,
        List<OrderPlacedEvent.Item> items
) {
    public static final String TYPE = "ORDER_CANCELLED";

    public static OrderCancelledEvent from(Order order, String cancelledBy){
        List<OrderPlacedEvent.Item> items = order.getItems().stream()
                .map(item -> new OrderPlacedEvent.Item(
                        item.getProductId(),
                        item.getSku(),
                        item.getQuantity(),
                        item.getUnitPrice()
                )).toList();

        return new OrderCancelledEvent(
                UUID.randomUUID().toString(),
                TYPE,
                Instant.now(),
                order.getId(),
                order.getUserEmail(),
                cancelledBy,
                items);

    }
}
