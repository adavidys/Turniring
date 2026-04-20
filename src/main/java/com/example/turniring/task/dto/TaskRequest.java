package com.example.turniring.task.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record TaskRequest(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        String technologyRequirements,

        List<String> mustHaveCriteria,

        String additionalMaterialsUrl,

        @NotNull(message = "Start time is required")
        LocalDateTime startAt,

        @NotNull(message = "Deadline is required")
        @Future(message = "Deadline must be in the future")
        LocalDateTime deadlineAt
) {
}
