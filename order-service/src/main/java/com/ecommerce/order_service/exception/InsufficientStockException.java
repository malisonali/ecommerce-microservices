package com.ecommerce.order_service.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String sku, int requested, int available) {
        super("Not enough stock for " + sku + ": requested " + requested + ", available " + available);
    }
}