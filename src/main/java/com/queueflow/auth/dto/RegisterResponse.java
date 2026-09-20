package com.queueflow.auth.dto;

import com.queueflow.user.UserRole;

public record RegisterResponse(
        Long userId,
        String name,
        String email,
        String phone,
        UserRole role
) {
}