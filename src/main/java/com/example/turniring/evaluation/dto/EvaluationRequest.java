package com.example.turniring.evaluation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EvaluationRequest(
        @NotNull @Min(0) @Max(100) Integer backendScore,
        @NotNull @Min(0) @Max(100) Integer databaseScore,
        @NotNull @Min(0) @Max(100) Integer frontendScore,
        @NotNull @Min(0) @Max(100) Integer mustHaveScore,
        @NotNull @Min(0) @Max(100) Integer functionalityScore,
        @NotNull @Min(0) @Max(100) Integer usabilityScore,
        String comment
) {
}
