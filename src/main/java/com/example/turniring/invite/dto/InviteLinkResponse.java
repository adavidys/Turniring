package com.example.turniring.invite.dto;

import com.example.turniring.invite.entity.InviteType;

import java.time.LocalDateTime;
import java.util.UUID;

public record InviteLinkResponse(
        UUID token,
        InviteType type,
        Long teamId,
        String teamName,
        LocalDateTime expiresAt,
        String invitePath
) {
}
