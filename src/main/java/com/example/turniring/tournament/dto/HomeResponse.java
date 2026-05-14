package com.example.turniring.tournament.dto;

import java.util.List;

public record HomeResponse(
        List<TournamentResponse> recommended,
        List<TournamentResponse> registrationOpen,
        List<TournamentResponse> running,
        List<TournamentResponse> finished
) {
}
