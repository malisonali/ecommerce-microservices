package com.ecommerce.product_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderPlacedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Long orderId,
        List<OrderPlacedEvent.Item> items
) {
    public static final String TYPE = "ORDER_PLACED";

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(Long productId, int quantity) {
    }
}
