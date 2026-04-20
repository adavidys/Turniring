package com.example.turniring.user.dto;

import com.example.turniring.user.entity.UserEntity;

public record AuthResponse(
        Long id,
        String name,
        String lastName,
        String email,
        String role,
        String token
) {
    public static AuthResponse from(UserEntity user, String token) {
        return new AuthResponse(
                user.getId(),
                user.getName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().name(),
                token
        );
    }
}
