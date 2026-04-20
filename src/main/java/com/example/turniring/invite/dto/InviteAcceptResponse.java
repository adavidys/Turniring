package com.example.turniring.invite.dto;

import com.example.turniring.invite.entity.InviteType;
import com.example.turniring.user.entity.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record InviteAcceptResponse(
        UUID token,
        InviteType type,
        Long teamId,
        String teamName,
        UserRole role,
        LocalDateTime acceptedAt
) {
}
