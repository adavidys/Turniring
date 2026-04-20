package com.example.turniring.submission.dto;

import com.example.turniring.submission.entity.SubmissionEntity;
import com.example.turniring.submission.entity.SubmissionStatus;

import java.time.LocalDateTime;

public record SubmissionResponse(
        Long id,
        Long taskId,
        String taskTitle,
        Long teamId,
        String teamName,
        String githubUrl,
        String demoVideoUrl,
        String liveDemoUrl,
        String summary,
        SubmissionStatus status,
        LocalDateTime submittedAt,
        LocalDateTime updatedAt
) {
    public static SubmissionResponse from(SubmissionEntity submission) {
        return new SubmissionResponse(
                submission.getId(),
                submission.getTask().getId(),
                submission.getTask().getTitle(),
                submission.getTeam().getId(),
                submission.getTeam().getName(),
                submission.getGithubUrl(),
                submission.getDemoVideoUrl(),
                submission.getLiveDemoUrl(),
                submission.getSummary(),
                submission.getStatus(),
                submission.getSubmittedAt(),
                submission.getUpdatedAt()
        );
    }
}
