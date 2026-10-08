package com.ecommerce.order_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InventoryEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Long orderId,
        String reason) {

    public static final String STOCK_RESERVED = "STOCK_RESERVED";
    public static final String STOCK_RESERVATION_FAILED = "STOCK_RESERVATION_FAILED";
}