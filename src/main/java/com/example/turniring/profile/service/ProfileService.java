package com.example.turniring.profile.service;

import com.example.turniring.evaluation.service.EvaluationService;
import com.example.turniring.profile.dto.ProfileResponse;
import com.example.turniring.profile.dto.UpdateProfileDataRequest;
import com.example.turniring.team.service.TeamService;
import com.example.turniring.tournament.service.TournamentService;
import com.example.turniring.user.entity.UserEntity;
import com.example.turniring.user.entity.UserRole;
import com.example.turniring.user.service.CurrentUserService;
import com.example.turniring.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final CurrentUserService currentUserService;
    private final TeamService teamService;
    private final TournamentService tournamentService;
    private final EvaluationService evaluationService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile() {
        UserEntity user = currentUserService.requireCurrentUser();
        return buildProfileResponse(user);
    }

    @Transactional
    public ProfileResponse updateMyRole(UserRole requestedRole) {
        UserEntity user = currentUserService.requireCurrentUser();
        if (requestedRole == user.getRole()) {
            return buildProfileResponse(user);
        }
        UserRole nextRole = userService.resolveSelfAssignableRole(requestedRole, user.getRole());
        if (nextRole == UserRole.TEAM) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "TEAM role is managed via team pages. Choose USER or ADMIN."
            );
        }
        if (teamService.isUserInAnyTeam(user)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Role cannot be changed while user is in a team"
            );
        }
        user.setRole(nextRole);
        return buildProfileResponse(user);
    }

    @Transactional
    public ProfileResponse updateMyData(UpdateProfileDataRequest request) {
        currentUserService.requireRole(UserRole.ADMIN, UserRole.JURY);
        UserEntity user = currentUserService.requireCurrentUser();
        userService.updateProfileData(
                user,
                request.name(),
                request.lastName(),
                request.email(),
                request.newPassword()
        );
        return buildProfileResponse(user);
    }

    private ProfileResponse buildProfileResponse(UserEntity user) {
        boolean inTeam = teamService.isUserInAnyTeam(user);
        return new ProfileResponse(
                user.getId(),
                user.getName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                inTeam,
                teamService.listMyTeams(),
                isManager(user.getRole()) ? tournamentService.listManagedTournaments(user.getId()) : List.of(),
                user.getRole() == UserRole.JURY ? evaluationService.getAssignmentsForJury(user.getId()) : List.of()
        );
    }

    private boolean isManager(UserRole role) {
        return role == UserRole.ADMIN || role == UserRole.ORGANIZER;
    }
}
