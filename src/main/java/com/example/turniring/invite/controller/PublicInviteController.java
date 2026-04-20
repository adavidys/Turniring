package com.example.turniring.invite.controller;

import com.example.turniring.invite.dto.InvitePublicResponse;
import com.example.turniring.invite.service.InviteService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/invites")
public class PublicInviteController {

    private final InviteService inviteService;

    @GetMapping("/{token}")
    @Operation(summary = "Get invite details by token")
    public InvitePublicResponse invite(@PathVariable UUID token) {
        return inviteService.getInvite(token);
    }
}
