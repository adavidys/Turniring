package com.example.turniring.evaluation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AssignEvaluationsRequest(
        @NotNull(message = "Evaluators per submission is required")
        @Min(value = 1, message = "Evaluators per submission must be at least 1")
        Integer evaluatorsPerSubmission,

        @Min(value = 1, message = "Max assignments per jury must be at least 1")
        Integer maxAssignmentsPerJury
) {
}
