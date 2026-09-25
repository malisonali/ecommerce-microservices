package com.ecommerce.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotNull(message = "Full name is required")
        String fullName,

        @NotNull(message = "Email is required")
        @Email(message = "Email format is invalid")
        String email,

        @NotNull(message = "Password is required")
        @Size(min = 8, message = "Password must be atleast 8 characters")
        String password
) {
}
