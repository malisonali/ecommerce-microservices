package com.ecommerce.notification_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Long orderId,
        String userEmail,
        BigDecimal totalAmount,
        String cancelledBy) {

    public static final String ORDER_PLACED = "ORDER_PLACED";
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";
}