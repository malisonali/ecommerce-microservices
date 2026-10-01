package com.ecommerce.product_service.dto;

import java.math.BigDecimal;

public record ProductFilter(
        String category,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String name,
        Boolean inStock
) {
}