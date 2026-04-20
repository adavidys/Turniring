package com.example.turniring.profile.dto;

import com.example.turniring.evaluation.dto.JuryAssignmentResponse;
import com.example.turniring.team.dto.TeamResponse;
import com.example.turniring.tournament.dto.TournamentResponse;
import com.example.turniring.user.entity.UserRole;

import java.util.List;

public record ProfileResponse(
        Long id,
        String name,
        String lastName,
        String email,
        UserRole role,
        boolean inTeam,
        List<TeamResponse> teams,
        List<TournamentResponse> managedTournaments,
        List<JuryAssignmentResponse> juryAssignments
) {
}
