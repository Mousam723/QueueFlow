package com.queueflow.auth;

import com.queueflow.user.UserRole;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Long userId,
        String name,
        String email,
        UserRole role
) {
}