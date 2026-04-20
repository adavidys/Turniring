package com.example.turniring.team.dto;

import com.example.turniring.team.entity.TeamEntity;

import java.time.LocalDateTime;
import java.util.List;

public record TeamResponse(
        Long id,
        Long tournamentId,
        String name,
        Long captainId,
        String captainName,
        String captainEmail,
        String city,
        String organization,
        String contactHandle,
        LocalDateTime createdAt,
        List<TeamMemberResponse> members
) {
    public static TeamResponse from(TeamEntity team, List<TeamMemberResponse> members) {
        return new TeamResponse(
                team.getId(),
                team.getTournament() == null ? null : team.getTournament().getId(),
                team.getName(),
                team.getCaptain().getId(),
                team.getCaptain().getName() + " " + team.getCaptain().getLastName(),
                team.getCaptain().getEmail(),
                team.getCity(),
                team.getOrganization(),
                team.getContactHandle(),
                team.getCreatedAt(),
                members
        );
    }
}
