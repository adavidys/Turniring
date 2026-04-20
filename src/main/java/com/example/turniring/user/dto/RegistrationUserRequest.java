package com.example.turniring.user.dto;

import com.example.turniring.user.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationUserRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 2, max = 255, message = "The name must be between 2 and 255 characters.")
        String name,

        @NotBlank(message = "Username is required")
        @Size(min = 2, max = 255, message = "The name must be between 2 and 255 characters.")
        String lastName,

        @NotBlank(message = "Email is required")
        @Schema(example = "user@example.com")
        @Email
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        String password,

        @Schema(description = "Requested role for self-registration", example = "TEAM")
        UserRole role
) {
}
