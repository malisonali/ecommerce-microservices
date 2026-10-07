package com.ecommerce.product_service.exception;

public class InsufficientStockException extends RuntimeException{
    private final Long productId;

    public InsufficientStockException(Long productId, int requested){
        super("Not enough stock for product " + productId + " (requested " + requested + ")");
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }
}
