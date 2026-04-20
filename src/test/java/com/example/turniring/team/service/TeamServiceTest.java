package com.example.turniring.team.service;

import com.example.turniring.invite.repository.InviteRepository;
import com.example.turniring.submission.repository.SubmissionRepository;
import com.example.turniring.support.TestFixtures;
import com.example.turniring.team.dto.TeamCreateRequest;
import com.example.turniring.team.dto.TeamMemberRequest;
import com.example.turniring.team.dto.TeamRegistrationRequest;
import com.example.turniring.team.entity.TeamEntity;
import com.example.turniring.team.entity.TeamMemberEntity;
import com.example.turniring.team.repository.TeamMemberRepository;
import com.example.turniring.team.repository.TeamRepository;
import com.example.turniring.tournament.entity.TournamentEntity;
import com.example.turniring.tournament.entity.TournamentStatus;
import com.example.turniring.tournament.service.TournamentService;
import com.example.turniring.user.entity.UserEntity;
import com.example.turniring.user.entity.UserRole;
import com.example.turniring.user.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private InviteRepository inviteRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private TournamentService tournamentService;
    @Mock
    private CurrentUserService currentUserService;

    private TeamService teamService;
    private LocalDateTime now;
    private UserEntity captain;
    private TournamentEntity tournament;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.of(2026, 4, 18, 14, 0);
        Clock clock = TestFixtures.fixedClock(now);
        teamService = new TeamService(
                teamRepository,
                teamMemberRepository,
                inviteRepository,
                submissionRepository,
                tournamentService,
                currentUserService,
                clock
        );

        captain = TestFixtures.user(10L, "captain@example.com", UserRole.TEAM);
        tournament = TestFixtures.tournament(
                20L,
                TournamentStatus.REGISTRATION,
                now.minusHours(1),
                now.plusDays(1),
                false,
                TestFixtures.user(1L, "admin@example.com", UserRole.ADMIN)
        );
    }

    @Test
    void createTeamPersistsTeamAndMembersWithClockTimestamp() {
        TeamCreateRequest request = new TeamCreateRequest(
                "Code Masters",
                "Kyiv",
                "Lyceum",
                "@coders"
        );

        when(currentUserService.requireCurrentUser()).thenReturn(captain);
        when(teamRepository.save(any(TeamEntity.class))).thenAnswer(invocation -> {
            TeamEntity team = invocation.getArgument(0);
            team.setId(99L);
            return team;
        });
        when(teamMemberRepository.findAllByTeamId(99L)).thenReturn(List.of(
                TeamMemberEntity.builder().id(1L).team(null).fullName("Test User").email("captain@example.com").captain(true).build()
        ));

        var response = teamService.createTeam(request);

        ArgumentCaptor<TeamEntity> captor = ArgumentCaptor.forClass(TeamEntity.class);
        verify(teamRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedAt()).isEqualTo(now);
        assertThat(captor.getValue().getTournament()).isNull();
        verify(teamMemberRepository, times(1)).save(any(TeamMemberEntity.class));
        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.members()).hasSize(1);
        assertThat(response.tournamentId()).isNull();
    }

    @Test
    void createTeamRejectsDuplicateName() {
        TeamCreateRequest request = new TeamCreateRequest(
                "Code Masters",
                "Kyiv",
                "Lyceum",
                "@coders"
        );
        when(currentUserService.requireCurrentUser()).thenReturn(captain);
        when(teamRepository.existsByNameIgnoreCase("Code Masters")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> teamService.createTeam(request)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception.getReason()).contains("Team name already exists");
        verify(teamRepository, never()).save(any(TeamEntity.class));
    }

    @Test
    void createTeamTrimsNameBeforeLookupAndPersist() {
        TeamCreateRequest request = new TeamCreateRequest(
                "  Code Masters  ",
                "Kyiv",
                "Lyceum",
                "@coders"
        );

        when(currentUserService.requireCurrentUser()).thenReturn(captain);
        when(teamRepository.save(any(TeamEntity.class))).thenAnswer(invocation -> {
            TeamEntity team = invocation.getArgument(0);
            team.setId(99L);
            return team;
        });
        when(teamMemberRepository.findAllByTeamId(99L)).thenReturn(List.of(
                TeamMemberEntity.builder().id(1L).team(null).fullName("Test User").email("captain@example.com").captain(true).build()
        ));

        teamService.createTeam(request);

        verify(teamRepository).existsByNameIgnoreCase("Code Masters");
        ArgumentCaptor<TeamEntity> captor = ArgumentCaptor.forClass(TeamEntity.class);
        verify(teamRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Code Masters");
    }

    @Test
    void createTeamRejectsBlankName() {
        TeamCreateRequest request = new TeamCreateRequest(
                "   ",
                "Kyiv",
                "Lyceum",
                "@coders"
        );
        when(currentUserService.requireCurrentUser()).thenReturn(captain);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> teamService.createTeam(request)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).contains("Team name is required");
        verify(teamRepository, never()).existsByNameIgnoreCase(any(String.class));
        verify(teamRepository, never()).save(any(TeamEntity.class));
    }

    @Test
    void joinTeamAttachesStandaloneTeamToTournament() {
        TeamEntity standaloneTeam = TestFixtures.team(70L, "Code Masters", null, captain, now.minusMinutes(10));
        when(currentUserService.requireCurrentUser()).thenReturn(captain);
        when(teamRepository.findById(70L)).thenReturn(java.util.Optional.of(standaloneTeam));
        when(tournamentService.getTournamentEntity(20L)).thenReturn(tournament);
        when(tournamentService.isRegistrationOpen(tournament)).thenReturn(true);
        when(teamRepository.existsByTournamentIdAndCaptainId(20L, 10L)).thenReturn(false);
        when(teamRepository.existsByTournamentIdAndName(20L, "Code Masters")).thenReturn(false);
        when(teamRepository.countByTournamentId(20L)).thenReturn(0L);
        when(teamMemberRepository.findAllByTeamId(70L)).thenReturn(List.of(
                TeamMemberEntity.builder().id(1L).fullName("Test User").email("captain@example.com").captain(true).build(),
                TeamMemberEntity.builder().id(2L).fullName("Bob").email("bob@example.com").captain(false).build()
        ));
        when(teamRepository.save(any(TeamEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = teamService.joinTeam(70L, 20L);

        assertThat(response.tournamentId()).isEqualTo(20L);
    }

    @Test
    void joinTeamRejectsDuplicateNameInTournament() {
        TeamEntity standaloneTeam = TestFixtures.team(70L, "Code Masters", null, captain, now.minusMinutes(10));
        when(currentUserService.requireCurrentUser()).thenReturn(captain);
        when(teamRepository.findById(70L)).thenReturn(java.util.Optional.of(standaloneTeam));
        when(tournamentService.getTournamentEntity(20L)).thenReturn(tournament);
        when(tournamentService.isRegistrationOpen(tournament)).thenReturn(true);
        when(teamRepository.existsByTournamentIdAndCaptainId(20L, 10L)).thenReturn(false);
        when(teamRepository.existsByTournamentIdAndName(20L, "Code Masters")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> teamService.joinTeam(70L, 20L)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception.getReason()).contains("already registered in this tournament");
        verify(teamRepository, never()).save(any(TeamEntity.class));
    }

    @Test
    void leaveTournamentRejectsAfterRegistrationClosed() {
        TeamEntity joinedTeam = TestFixtures.team(80L, "Code Masters", tournament, captain, now.minusDays(1));
        tournament.setRegistrationEndAt(now.minusMinutes(1));
        when(currentUserService.requireCurrentUser()).thenReturn(captain);
        when(teamRepository.findById(80L)).thenReturn(java.util.Optional.of(joinedTeam));
        when(tournamentService.isRegistrationOpen(tournament)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> teamService.leaveTournament(80L)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).contains("only while registration is open");
    }

    @Test
    void deleteTeamRemovesTeamMembersInvitesAndTeam() {
        TeamEntity standaloneTeam = TestFixtures.team(90L, "Code Masters", null, captain, now.minusDays(1));
        when(currentUserService.requireCurrentUser()).thenReturn(captain);
        when(teamRepository.findById(90L)).thenReturn(java.util.Optional.of(standaloneTeam));
        when(submissionRepository.existsByTeamId(90L)).thenReturn(false);

        teamService.deleteTeam(90L);

        verify(inviteRepository).deleteAllByTeamId(90L);
        verify(teamMemberRepository).deleteAllByTeamId(90L);
        verify(teamRepository).delete(standaloneTeam);
    }

    @Test
    void deleteTeamRejectsAfterRegistrationClosed() {
        TeamEntity joinedTeam = TestFixtures.team(91L, "Code Masters", tournament, captain, now.minusDays(1));
        tournament.setRegistrationEndAt(now.minusMinutes(1));
        when(currentUserService.requireCurrentUser()).thenReturn(captain);
        when(teamRepository.findById(91L)).thenReturn(java.util.Optional.of(joinedTeam));
        when(tournamentService.isRegistrationOpen(tournament)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> teamService.deleteTeam(91L)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).contains("only while registration is open");
        verify(teamRepository, never()).delete(any(TeamEntity.class));
    }

    @Test
    void deleteTeamRejectsWhenSubmissionsExist() {
        TeamEntity standaloneTeam = TestFixtures.team(92L, "Code Masters", null, captain, now.minusDays(1));
        when(currentUserService.requireCurrentUser()).thenReturn(captain);
        when(teamRepository.findById(92L)).thenReturn(java.util.Optional.of(standaloneTeam));
        when(submissionRepository.existsByTeamId(92L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> teamService.deleteTeam(92L)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).contains("with submissions cannot be deleted");
        verify(teamRepository, never()).delete(any(TeamEntity.class));
    }

    @Test
    void updateTeamRejectsEditingAfterRegistrationForCaptain() {
        TeamEntity team = TestFixtures.team(50L, "Old Name", tournament, captain, now.minusDays(1));
        tournament.setRegistrationEndAt(now.minusMinutes(1));
        TeamRegistrationRequest request = new TeamRegistrationRequest(
                "New Name",
                "Lviv",
                "Gymnasium",
                "@new",
                List.of(new TeamMemberRequest("Bob", "bob@example.com"))
        );

        when(currentUserService.requireCurrentUser()).thenReturn(captain);
        when(teamRepository.findById(50L)).thenReturn(java.util.Optional.of(team));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> teamService.updateTeam(50L, request)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).contains("editing is closed");
    }

    @Test
    void updateStandaloneTeamAllowsNoAdditionalMembers() {
        TeamEntity team = TestFixtures.team(51L, "Old Name", null, captain, now.minusDays(1));
        TeamRegistrationRequest request = new TeamRegistrationRequest(
                "New Name",
                "Lviv",
                "Gymnasium",
                "@new",
                List.of()
        );

        when(currentUserService.requireCurrentUser()).thenReturn(captain);
        when(teamRepository.findById(51L)).thenReturn(java.util.Optional.of(team));
        when(teamRepository.save(any(TeamEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(teamMemberRepository.findAllByTeamId(51L)).thenReturn(List.of(
                TeamMemberEntity.builder().id(1L).team(null).fullName("Test User").email("captain@example.com").captain(true).build()
        ));

        var response = teamService.updateTeam(51L, request);

        verify(teamMemberRepository, times(1)).save(any(TeamMemberEntity.class));
        assertThat(response.members()).hasSize(1);
        assertThat(response.members().getFirst().captain()).isTrue();
    }

    @Test
    void updateTeamRejectsDuplicateName() {
        TeamEntity team = TestFixtures.team(51L, "Old Name", null, captain, now.minusDays(1));
        TeamRegistrationRequest request = new TeamRegistrationRequest(
                "New Name",
                "Lviv",
                "Gymnasium",
                "@new",
                List.of()
        );

        when(currentUserService.requireCurrentUser()).thenReturn(captain);
        when(teamRepository.findById(51L)).thenReturn(java.util.Optional.of(team));
        when(teamRepository.existsByNameIgnoreCaseAndIdNot("New Name", 51L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> teamService.updateTeam(51L, request)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception.getReason()).contains("Team name already exists");
    }

    @Test
    void updateTeamTrimsNameBeforeLookupAndPersist() {
        TeamEntity team = TestFixtures.team(51L, "Old Name", null, captain, now.minusDays(1));
        TeamRegistrationRequest request = new TeamRegistrationRequest(
                "  New Name  ",
                "Lviv",
                "Gymnasium",
                "@new",
                List.of()
        );

        when(currentUserService.requireCurrentUser()).thenReturn(captain);
        when(teamRepository.findById(51L)).thenReturn(java.util.Optional.of(team));
        when(teamRepository.save(any(TeamEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(teamMemberRepository.findAllByTeamId(51L)).thenReturn(List.of(
                TeamMemberEntity.builder().id(1L).team(null).fullName("Test User").email("captain@example.com").captain(true).build()
        ));

        teamService.updateTeam(51L, request);

        verify(teamRepository).existsByNameIgnoreCaseAndIdNot("New Name", 51L);
        assertThat(team.getName()).isEqualTo("New Name");
    }

    @Test
    void updateTeamRejectsBlankName() {
        TeamEntity team = TestFixtures.team(51L, "Old Name", null, captain, now.minusDays(1));
        TeamRegistrationRequest request = new TeamRegistrationRequest(
                "   ",
                "Lviv",
                "Gymnasium",
                "@new",
                List.of()
        );

        when(currentUserService.requireCurrentUser()).thenReturn(captain);
        when(teamRepository.findById(51L)).thenReturn(java.util.Optional.of(team));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> teamService.updateTeam(51L, request)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).contains("Team name is required");
        verify(teamRepository, never()).existsByNameIgnoreCaseAndIdNot(any(String.class), any(Long.class));
    }
}
