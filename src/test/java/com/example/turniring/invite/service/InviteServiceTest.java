package com.example.turniring.invite.service;

import com.example.turniring.invite.entity.InviteEntity;
import com.example.turniring.invite.entity.InviteType;
import com.example.turniring.invite.repository.InviteRepository;
import com.example.turniring.support.TestFixtures;
import com.example.turniring.team.entity.TeamEntity;
import com.example.turniring.team.entity.TeamMemberEntity;
import com.example.turniring.team.repository.TeamMemberRepository;
import com.example.turniring.team.service.TeamService;
import com.example.turniring.tournament.entity.TournamentEntity;
import com.example.turniring.tournament.entity.TournamentStatus;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InviteServiceTest {

    @Mock
    private InviteRepository inviteRepository;
    @Mock
    private TeamService teamService;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private CurrentUserService currentUserService;

    private InviteService inviteService;
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = TestFixtures.fixedClock(LocalDateTime.of(2026, 1, 15, 10, 0));
        inviteService = new InviteService(
                inviteRepository,
                teamService,
                teamMemberRepository,
                currentUserService,
                clock
        );
        ReflectionTestUtils.setField(inviteService, "inviteExpirationHours", 1L);
    }

    @Test
    void createJuryInviteBuildsUuidLink() {
        UserEntity admin = TestFixtures.user(1L, "admin@example.com", UserRole.ADMIN);
        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        when(inviteRepository.save(any(InviteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = inviteService.createJuryInvite();

        assertThat(result.type()).isEqualTo(InviteType.JURY);
        assertThat(result.invitePath()).isEqualTo("/invite/" + result.token());
        assertThat(result.expiresAt()).isEqualTo(LocalDateTime.of(2026, 1, 15, 11, 0));
        verify(inviteRepository).save(any(InviteEntity.class));
    }

    @Test
    void acceptJuryInviteRejectsUserThatIsInTeam() {
        UserEntity user = TestFixtures.user(2L, "member@example.com", UserRole.USER);
        UUID token = UUID.randomUUID();
        InviteEntity invite = InviteEntity.builder()
                .token(token)
                .type(InviteType.JURY)
                .createdBy(TestFixtures.user(3L, "admin@example.com", UserRole.ADMIN))
                .createdAt(LocalDateTime.of(2026, 1, 15, 10, 0))
                .expiresAt(LocalDateTime.of(2026, 1, 17, 10, 0))
                .build();

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(inviteRepository.findByToken(token)).thenReturn(Optional.of(invite));
        when(teamService.isUserInAnyTeam(user)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> inviteService.acceptInvite(token));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo("User in team cannot accept jury invite");
        verify(inviteRepository, never()).delete(any(InviteEntity.class));
    }

    @Test
    void acceptTeamInviteAddsMemberAndSwitchesRoleToUser() {
        UserEntity user = TestFixtures.user(4L, "jury@example.com", UserRole.JURY);
        TournamentEntity tournament = TestFixtures.tournament(
                11L,
                TournamentStatus.REGISTRATION,
                LocalDateTime.of(2026, 1, 10, 8, 0),
                LocalDateTime.of(2026, 1, 20, 8, 0),
                false,
                TestFixtures.user(9L, "organizer@example.com", UserRole.ADMIN)
        );
        TeamEntity team = TestFixtures.team(15L, "Blue Team", tournament, TestFixtures.user(8L, "captain@example.com", UserRole.USER), LocalDateTime.of(2026, 1, 11, 10, 0));
        UUID token = UUID.randomUUID();
        InviteEntity invite = InviteEntity.builder()
                .token(token)
                .type(InviteType.TEAM)
                .team(team)
                .createdBy(TestFixtures.user(3L, "admin@example.com", UserRole.ADMIN))
                .createdAt(LocalDateTime.of(2026, 1, 15, 10, 0))
                .expiresAt(LocalDateTime.of(2026, 1, 17, 10, 0))
                .build();

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(inviteRepository.findByToken(token)).thenReturn(Optional.of(invite));
        when(teamMemberRepository.existsByTeamIdAndEmail(15L, "jury@example.com")).thenReturn(false);
        when(teamMemberRepository.countByTeamId(15L)).thenReturn(2L);

        var response = inviteService.acceptInvite(token);

        assertThat(response.type()).isEqualTo(InviteType.TEAM);
        assertThat(response.teamId()).isEqualTo(15L);
        assertThat(response.teamName()).isEqualTo("Blue Team");
        assertThat(response.role()).isEqualTo(UserRole.USER);
        assertThat(user.getRole()).isEqualTo(UserRole.USER);

        ArgumentCaptor<TeamMemberEntity> memberCaptor = ArgumentCaptor.forClass(TeamMemberEntity.class);
        verify(teamMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getTeam().getId()).isEqualTo(15L);
        assertThat(memberCaptor.getValue().getEmail()).isEqualTo("jury@example.com");
        assertThat(memberCaptor.getValue().isCaptain()).isFalse();
        verify(inviteRepository).delete(invite);
    }

    @Test
    void getInviteRejectsExpiredLinkAsInactive() {
        UUID token = UUID.randomUUID();
        InviteEntity invite = InviteEntity.builder()
                .token(token)
                .type(InviteType.JURY)
                .createdBy(TestFixtures.user(3L, "admin@example.com", UserRole.ADMIN))
                .createdAt(LocalDateTime.of(2026, 1, 15, 8, 0))
                .expiresAt(LocalDateTime.of(2026, 1, 15, 9, 0))
                .build();
        when(inviteRepository.findByToken(token)).thenReturn(Optional.of(invite));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> inviteService.getInvite(token));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo("This link is not active");
    }
}
