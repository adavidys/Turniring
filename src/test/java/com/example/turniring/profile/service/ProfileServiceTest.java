package com.example.turniring.profile.service;

import com.example.turniring.evaluation.service.EvaluationService;
import com.example.turniring.profile.dto.UpdateProfileDataRequest;
import com.example.turniring.support.TestFixtures;
import com.example.turniring.team.service.TeamService;
import com.example.turniring.tournament.service.TournamentService;
import com.example.turniring.user.entity.UserEntity;
import com.example.turniring.user.entity.UserRole;
import com.example.turniring.user.service.CurrentUserService;
import com.example.turniring.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private TeamService teamService;
    @Mock
    private TournamentService tournamentService;
    @Mock
    private EvaluationService evaluationService;
    @Mock
    private UserService userService;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(
                currentUserService,
                teamService,
                tournamentService,
                evaluationService,
                userService
        );
    }

    @Test
    void updateMyRoleChangesCurrentUserRoleAndReturnsUpdatedProfile() {
        UserEntity currentUser = TestFixtures.user(7L, "player@example.com", UserRole.TEAM);
        when(currentUserService.requireCurrentUser()).thenReturn(currentUser);
        when(userService.resolveSelfAssignableRole(UserRole.ADMIN, UserRole.TEAM)).thenReturn(UserRole.ADMIN);
        when(teamService.isUserInAnyTeam(currentUser)).thenReturn(false);
        when(teamService.listMyTeams()).thenReturn(List.of());
        when(tournamentService.listManagedTournaments(7L)).thenReturn(List.of());

        var result = profileService.updateMyRole(UserRole.ADMIN);

        assertThat(currentUser.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(result.role()).isEqualTo(UserRole.ADMIN);
        assertThat(result.inTeam()).isFalse();
        verify(tournamentService).listManagedTournaments(7L);
        verify(teamService).listMyTeams();
        verify(evaluationService, never()).getAssignmentsForJury(anyLong());
    }

    @Test
    void updateMyRoleKeepsExistingRoleWhenRequestedRoleIsSame() {
        UserEntity currentUser = TestFixtures.user(9L, "jury@example.com", UserRole.JURY);
        when(currentUserService.requireCurrentUser()).thenReturn(currentUser);
        when(teamService.isUserInAnyTeam(currentUser)).thenReturn(false);
        when(teamService.listMyTeams()).thenReturn(List.of());
        when(evaluationService.getAssignmentsForJury(9L)).thenReturn(List.of());

        var result = profileService.updateMyRole(UserRole.JURY);

        assertThat(result.role()).isEqualTo(UserRole.JURY);
        assertThat(result.inTeam()).isFalse();
        verify(evaluationService).getAssignmentsForJury(9L);
        verify(teamService).listMyTeams();
        verify(userService, never()).resolveSelfAssignableRole(any(), any());
        verify(tournamentService, never()).listManagedTournaments(anyLong());
    }

    @Test
    void updateMyRoleRejectsTeamRoleSwitch() {
        UserEntity currentUser = TestFixtures.user(11L, "team@example.com", UserRole.USER);
        when(currentUserService.requireCurrentUser()).thenReturn(currentUser);
        when(userService.resolveSelfAssignableRole(UserRole.TEAM, UserRole.USER)).thenReturn(UserRole.TEAM);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> profileService.updateMyRole(UserRole.TEAM)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo("TEAM role is managed via team pages. Choose USER or ADMIN.");
        verify(teamService, never()).listMyTeams();
        verify(tournamentService, never()).listManagedTournaments(anyLong());
        verify(evaluationService, never()).getAssignmentsForJury(anyLong());
    }

    @Test
    void updateMyRoleRejectsSwitchWhenUserBelongsToTeam() {
        UserEntity currentUser = TestFixtures.user(12L, "member@example.com", UserRole.USER);
        when(currentUserService.requireCurrentUser()).thenReturn(currentUser);
        when(userService.resolveSelfAssignableRole(UserRole.ADMIN, UserRole.USER)).thenReturn(UserRole.ADMIN);
        when(teamService.isUserInAnyTeam(currentUser)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> profileService.updateMyRole(UserRole.ADMIN)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo("Role cannot be changed while user is in an active team");
        assertThat(currentUser.getRole()).isEqualTo(UserRole.USER);
        verify(teamService, never()).listMyTeams();
        verify(tournamentService, never()).listManagedTournaments(anyLong());
        verify(evaluationService, never()).getAssignmentsForJury(anyLong());
    }

    @Test
    void updateMyRoleRejectsSwitchWhenManagerHasUnfinishedOlympiads() {
        UserEntity currentUser = TestFixtures.user(15L, "admin@example.com", UserRole.ADMIN);
        when(currentUserService.requireCurrentUser()).thenReturn(currentUser);
        when(userService.resolveSelfAssignableRole(UserRole.USER, UserRole.ADMIN)).thenReturn(UserRole.USER);
        when(teamService.isUserInAnyTeam(currentUser)).thenReturn(false);
        when(tournamentService.hasUnfinishedManagedTournament(15L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> profileService.updateMyRole(UserRole.USER)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo("Role cannot be changed while user has unfinished olympiads");
        assertThat(currentUser.getRole()).isEqualTo(UserRole.ADMIN);
        verify(teamService, never()).listMyTeams();
        verify(tournamentService, never()).listManagedTournaments(anyLong());
        verify(evaluationService, never()).getAssignmentsForJury(anyLong());
    }

    @Test
    void updateMyDataAllowsAdminAndReturnsUpdatedProfile() {
        UserEntity currentUser = TestFixtures.user(13L, "admin@example.com", UserRole.ADMIN);
        UpdateProfileDataRequest request = new UpdateProfileDataRequest(
                "Admin",
                "Updated",
                "updated@example.com",
                "password123"
        );

        when(currentUserService.requireCurrentUser()).thenReturn(currentUser);
        when(teamService.isUserInAnyTeam(currentUser)).thenReturn(false);
        when(teamService.listMyTeams()).thenReturn(List.of());
        when(tournamentService.listManagedTournaments(13L)).thenReturn(List.of());

        var result = profileService.updateMyData(request);

        verify(currentUserService).requireRole(UserRole.ADMIN, UserRole.JURY);
        verify(userService).updateProfileData(
                currentUser,
                "Admin",
                "Updated",
                "updated@example.com",
                "password123"
        );
        assertThat(result.role()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void updateMyDataRejectsRegularUserRole() {
        UserEntity currentUser = TestFixtures.user(14L, "user@example.com", UserRole.USER);
        UpdateProfileDataRequest request = new UpdateProfileDataRequest(
                "User",
                "Updated",
                "user@example.com",
                null
        );
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions"))
                .when(currentUserService).requireRole(UserRole.ADMIN, UserRole.JURY);
        when(currentUserService.requireCurrentUser()).thenReturn(currentUser);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> profileService.updateMyData(request)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(userService, never()).updateProfileData(any(), anyString(), anyString(), anyString(), any());
    }
}
