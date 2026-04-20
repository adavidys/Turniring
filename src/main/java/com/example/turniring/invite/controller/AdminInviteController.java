package com.example.turniring.invite.controller;

import com.example.turniring.invite.dto.InviteLinkResponse;
import com.example.turniring.invite.service.InviteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/invites")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
public class AdminInviteController {

    private final InviteService inviteService;

    @PostMapping("/jury")
    @Operation(summary = "Create jury invite link")
    public InviteLinkResponse createJuryInvite() {
        return inviteService.createJuryInvite();
    }

    @PostMapping("/teams/{teamId}")
    @Operation(summary = "Create team invite link")
    public InviteLinkResponse createTeamInvite(@PathVariable Long teamId) {
        return inviteService.createTeamInvite(teamId);
    }
}
