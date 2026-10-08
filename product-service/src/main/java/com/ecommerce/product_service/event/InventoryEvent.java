package com.ecommerce.product_service.event;

import java.time.Instant;
import java.util.UUID;

public record InventoryEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Long orderId,
        String reason) {

    public static final String STOCK_RESERVED = "STOCK_RESERVED";
    public static final String STOCK_RESERVATION_FAILED = "STOCK_RESERVATION_FAILED";

    public static InventoryEvent reserved(Long orderId) {
        return new InventoryEvent(UUID.randomUUID().toString(), STOCK_RESERVED,
                Instant.now(), orderId, null);
    }

    public static InventoryEvent failed(Long orderId, String reason) {
        return new InventoryEvent(UUID.randomUUID().toString(), STOCK_RESERVATION_FAILED,
                Instant.now(), orderId, reason);
    }
}