package com.ecommerce.product_service.exception;

public class InvalidQueryParameterException extends RuntimeException{
    public InvalidQueryParameterException(String message) {
        super(message);
    }
}
