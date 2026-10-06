package com.ecommerce.order_service.event;

import com.ecommerce.order_service.entity.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderPlacedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Long orderId,
        String userEmail,
        BigDecimal totalAmount,
        List<Item> items) {

    public static final String TYPE = "ORDER_PLACED";

    public record Item(Long productId, String sku, int quantity, BigDecimal unitPrice) {
    }

    public static OrderPlacedEvent from(Order order) {
        List<Item> items = order.getItems().stream()
                .map(item -> new Item(
                        item.getProductId(),
                        item.getSku(),
                        item.getQuantity(),
                        item.getUnitPrice()))
                .toList();

        return new OrderPlacedEvent(
                UUID.randomUUID().toString(),
                TYPE,
                Instant.now(),
                order.getId(),
                order.getUserEmail(),
                order.getTotalAmount(),
                items);
    }
}