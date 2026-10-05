package com.ecommerce.order_service.exception;

import com.ecommerce.order_service.entity.OrderStatus;

public class InvalidOrderStatusTransitionException extends RuntimeException{

    public InvalidOrderStatusTransitionException(Long orderId, OrderStatus from, OrderStatus to) {
        super("Order " + orderId + " cannot move from " + from + " to " + to);
    }
}
