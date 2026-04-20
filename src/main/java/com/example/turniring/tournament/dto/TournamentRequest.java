package com.example.turniring.tournament.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record TournamentRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must be at most 255 characters")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        String rules,

        LocalDateTime startAt,

        @NotNull(message = "Registration start time is required")
        @FutureOrPresent(message = "Registration start time must be in the present or future")
        LocalDateTime registrationStartAt,

        @NotNull(message = "Registration end time is required")
        @Future(message = "Registration end time must be in the future")
        LocalDateTime registrationEndAt,

        @Min(value = 1, message = "Max teams must be at least 1")
        Integer maxTeams,

        @NotNull(message = "Minimum rounds is required")
        @Min(value = 1, message = "Minimum rounds must be at least 1")
        Integer minimumRounds,

        @NotNull(message = "Minimum team members is required")
        @Min(value = 1, message = "Minimum team members must be at least 1")
        Integer teamMinMembers,

        @NotNull(message = "Maximum team members is required")
        @Min(value = 1, message = "Maximum team members must be at least 1")
        Integer teamMaxMembers,

        boolean hideTeamsUntilRegistrationEnds
) {
}
