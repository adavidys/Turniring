package com.example.turniring.team.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TeamRegistrationRequest(
        @NotBlank(message = "Team name is required")
        @Size(min = 2, max = 255, message = "Team name must be between 2 and 255 characters")
        String name,

        String city,

        String organization,

        String contactHandle,

        @Valid
        @NotNull(message = "Members list is required")
        List<TeamMemberRequest> members
) {
}
