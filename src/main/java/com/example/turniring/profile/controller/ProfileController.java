package com.example.turniring.profile.controller;

import com.example.turniring.invite.dto.InviteAcceptResponse;
import com.example.turniring.profile.dto.UpdateProfileDataRequest;
import com.example.turniring.invite.service.InviteService;
import com.example.turniring.profile.dto.ProfileResponse;
import com.example.turniring.profile.dto.UpdateProfileRoleRequest;
import com.example.turniring.profile.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profile")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;
    private final InviteService inviteService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ProfileResponse me() {
        return profileService.getMyProfile();
    }

    @PutMapping("/me/role")
    @Operation(summary = "Update current user role")
    public ProfileResponse updateMyRole(@Valid @RequestBody UpdateProfileRoleRequest request) {
        return profileService.updateMyRole(request.role());
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile data")
    public ProfileResponse updateMyData(@Valid @RequestBody UpdateProfileDataRequest request) {
        return profileService.updateMyData(request);
    }

    @PostMapping("/invites/{token}/accept")
    @Operation(summary = "Accept invite by token")
    public InviteAcceptResponse acceptInvite(@PathVariable UUID token) {
        return inviteService.acceptInvite(token);
    }
}
