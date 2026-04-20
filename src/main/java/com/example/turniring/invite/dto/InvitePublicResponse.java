package com.example.turniring.invite.dto;

import com.example.turniring.invite.entity.InviteType;

import java.time.LocalDateTime;
import java.util.UUID;

public record InvitePublicResponse(
        UUID token,
        InviteType type,
        Long teamId,
        String teamName,
        boolean used,
        boolean expired,
        LocalDateTime expiresAt
) {
}
