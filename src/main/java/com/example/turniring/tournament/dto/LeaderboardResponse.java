package com.example.turniring.tournament.dto;

import java.util.List;

public record LeaderboardResponse(
        Long tournamentId,
        String tournamentTitle,
        String scoringFormula,
        List<LeaderboardEntryResponse> entries
) {
}
