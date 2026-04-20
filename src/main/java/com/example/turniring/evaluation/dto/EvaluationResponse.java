package com.example.turniring.evaluation.dto;

import com.example.turniring.evaluation.entity.EvaluationEntity;

import java.time.LocalDateTime;

public record EvaluationResponse(
        Long id,
        Integer backendScore,
        Integer databaseScore,
        Integer frontendScore,
        Integer mustHaveScore,
        Integer functionalityScore,
        Integer usabilityScore,
        String comment,
        Double totalScore,
        LocalDateTime submittedAt
) {
    public static EvaluationResponse from(EvaluationEntity evaluation) {
        return new EvaluationResponse(
                evaluation.getId(),
                evaluation.getBackendScore(),
                evaluation.getDatabaseScore(),
                evaluation.getFrontendScore(),
                evaluation.getMustHaveScore(),
                evaluation.getFunctionalityScore(),
                evaluation.getUsabilityScore(),
                evaluation.getComment(),
                evaluation.getTotalScore(),
                evaluation.getSubmittedAt()
        );
    }
}
