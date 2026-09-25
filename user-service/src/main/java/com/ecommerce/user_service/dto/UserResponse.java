package com.ecommerce.user_service.dto;

import com.ecommerce.user_service.entity.Role;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        LocalDateTime createdAt
) {
}
