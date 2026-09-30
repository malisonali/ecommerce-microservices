package com.ecommerce.product_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String description,
        String sku,
        BigDecimal price,
        Integer stockQuantity,
        String category,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
