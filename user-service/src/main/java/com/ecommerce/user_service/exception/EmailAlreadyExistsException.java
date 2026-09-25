package com.ecommerce.user_service.exception;

public class EmailAlreadyExistsException extends RuntimeException{

    public EmailAlreadyExistsException(String email){
        super("Email already registered: " + email);
    }
}
