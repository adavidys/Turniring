package com.example.turniring.tournament.dto;

import java.util.List;

public record LeaderboardEntryResponse(
        int position,
        Long teamId,
        String teamName,
        String captainEmail,
        double totalScore,
        List<LeaderboardTaskScoreResponse> taskScores
) {
}
