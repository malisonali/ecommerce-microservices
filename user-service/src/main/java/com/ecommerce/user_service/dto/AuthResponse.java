package com.ecommerce.user_service.dto;

public record AuthResponse(
        String accessToken,
        String tokenType
) {
    public AuthResponse(String accessToken){
        this(accessToken, "Bearer");
    }
}
