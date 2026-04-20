package com.example.turniring.profile.dto;

import com.example.turniring.user.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateProfileRoleRequest(
        @NotNull(message = "Role is required")
        UserRole role
) {
}
