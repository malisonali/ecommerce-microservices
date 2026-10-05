package com.ecommerce.order_service.dto;

import com.ecommerce.order_service.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(

        @NotNull(message = "Status is required")
        OrderStatus status
) {
}