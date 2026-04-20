package com.example.turniring.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamCreateRequest(
        @NotBlank(message = "Team name is required")
        @Size(min = 2, max = 255, message = "Team name must be between 2 and 255 characters")
        String name,

        String city,

        String organization,

        String contactHandle
) {
}
