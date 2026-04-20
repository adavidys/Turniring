package com.example.turniring.tournament.dto;

public record LeaderboardTaskScoreResponse(
        Long taskId,
        String taskTitle,
        double averageScore,
        long evaluationsCount
) {
}
