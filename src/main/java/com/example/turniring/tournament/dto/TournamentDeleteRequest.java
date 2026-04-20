package com.example.turniring.tournament.dto;

import jakarta.validation.constraints.NotBlank;

public record TournamentDeleteRequest(
        @NotBlank(message = "Confirmation text is required")
        String confirmationText
) {
}
