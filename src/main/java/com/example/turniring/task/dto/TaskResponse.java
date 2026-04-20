package com.example.turniring.task.dto;

import com.example.turniring.task.entity.TaskEntity;
import com.example.turniring.task.entity.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;

public record TaskResponse(
        Long id,
        Long tournamentId,
        String title,
        String description,
        String technologyRequirements,
        List<String> mustHaveCriteria,
        String additionalMaterialsUrl,
        LocalDateTime startAt,
        LocalDateTime deadlineAt,
        TaskStatus status
) {
    public static TaskResponse from(TaskEntity task, List<String> mustHaveCriteria) {
        return new TaskResponse(
                task.getId(),
                task.getTournament().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getTechnologyRequirements(),
                mustHaveCriteria,
                task.getAdditionalMaterialsUrl(),
                task.getStartAt(),
                task.getDeadlineAt(),
                task.getStatus()
        );
    }
}
