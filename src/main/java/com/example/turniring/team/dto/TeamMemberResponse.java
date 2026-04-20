package com.example.turniring.team.dto;

import com.example.turniring.team.entity.TeamMemberEntity;

public record TeamMemberResponse(
        Long id,
        String fullName,
        String email,
        boolean captain
) {
    public static TeamMemberResponse from(TeamMemberEntity member) {
        return new TeamMemberResponse(
                member.getId(),
                member.getFullName(),
                member.getEmail(),
                member.isCaptain()
        );
    }
}
