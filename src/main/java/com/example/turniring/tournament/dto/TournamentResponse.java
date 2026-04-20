package com.example.turniring.tournament.dto;

import com.example.turniring.tournament.entity.TournamentEntity;
import com.example.turniring.tournament.entity.TournamentStatus;

import java.time.LocalDateTime;

public record TournamentResponse(
        Long id,
        String title,
        String description,
        String rules,
        LocalDateTime startAt,
        LocalDateTime registrationStartAt,
        LocalDateTime registrationEndAt,
        Integer maxTeams,
        Integer minimumRounds,
        Integer teamMinMembers,
        Integer teamMaxMembers,
        boolean hideTeamsUntilRegistrationEnds,
        TournamentStatus status,
        long registeredTeams,
        boolean registrationOpen,
        boolean teamsVisible
) {
    public static TournamentResponse from(
            TournamentEntity tournament,
            long registeredTeams,
            boolean registrationOpen,
            boolean teamsVisible
    ) {
        return new TournamentResponse(
                tournament.getId(),
                tournament.getTitle(),
                tournament.getDescription(),
                tournament.getRules(),
                tournament.getStartAt(),
                tournament.getRegistrationStartAt(),
                tournament.getRegistrationEndAt(),
                tournament.getMaxTeams(),
                tournament.getMinimumRounds(),
                tournament.getTeamMinMembers(),
                tournament.getTeamMaxMembers(),
                tournament.isHideTeamsUntilRegistrationEnds(),
                tournament.getStatus(),
                registeredTeams,
                registrationOpen,
                teamsVisible
        );
    }
}
