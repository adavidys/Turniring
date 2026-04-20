package com.example.turniring.tournament.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ScheduleEventRequest(
        @NotBlank(message = "Title is required")
        String title,

        String description,

        @NotNull(message = "Start time is required")
        LocalDateTime startAt,

        @NotNull(message = "End time is required")
        @Future(message = "End time must be in the future")
        LocalDateTime endAt,

        String link
) {
}
