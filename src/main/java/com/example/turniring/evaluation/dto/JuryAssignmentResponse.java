package com.example.turniring.evaluation.dto;

import com.example.turniring.evaluation.entity.EvaluationAssignmentEntity;
import com.example.turniring.evaluation.entity.EvaluationAssignmentStatus;
import com.example.turniring.submission.dto.SubmissionResponse;

import java.time.LocalDateTime;

public record JuryAssignmentResponse(
        Long assignmentId,
        EvaluationAssignmentStatus status,
        LocalDateTime assignedAt,
        SubmissionResponse submission,
        EvaluationResponse evaluation
) {
    public static JuryAssignmentResponse from(
            EvaluationAssignmentEntity assignment,
            SubmissionResponse submission,
            EvaluationResponse evaluation
    ) {
        return new JuryAssignmentResponse(
                assignment.getId(),
                assignment.getStatus(),
                assignment.getAssignedAt(),
                submission,
                evaluation
        );
    }
}
