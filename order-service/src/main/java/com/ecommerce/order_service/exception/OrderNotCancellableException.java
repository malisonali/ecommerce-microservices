package com.ecommerce.order_service.exception;

import com.ecommerce.order_service.entity.OrderStatus;

public class OrderNotCancellableException extends RuntimeException {

    public OrderNotCancellableException(Long orderId, OrderStatus status) {
        super("Order " + orderId + " is " + status + " and can no longer be cancelled");
    }
}