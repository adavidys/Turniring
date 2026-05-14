package com.example.turniring.tournament.service;

import com.example.turniring.announcement.entity.AnnouncementEntity;
import com.example.turniring.announcement.repository.AnnouncementRepository;
import com.example.turniring.evaluation.repository.EvaluationAssignmentRepository;
import com.example.turniring.evaluation.repository.EvaluationRepository;
import com.example.turniring.schedule.repository.ScheduleEventRepository;
import com.example.turniring.submission.repository.SubmissionRepository;
import com.example.turniring.support.TestFixtures;
import com.example.turniring.task.repository.TaskRepository;
import com.example.turniring.team.repository.TeamRepository;
import com.example.turniring.tournament.dto.AnnouncementRequest;
import com.example.turniring.tournament.dto.TournamentRequest;
import com.example.turniring.tournament.dto.TournamentResponse;
import com.example.turniring.tournament.entity.TournamentEntity;
import com.example.turniring.tournament.entity.TournamentLikeEntity;
import com.example.turniring.tournament.entity.TournamentStatus;
import com.example.turniring.tournament.repository.TournamentLikeRepository;
import com.example.turniring.tournament.repository.TournamentRepository;
import com.example.turniring.user.entity.UserEntity;
import com.example.turniring.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private AnnouncementRepository announcementRepository;
    @Mock
    private ScheduleEventRepository scheduleEventRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private EvaluationAssignmentRepository evaluationAssignmentRepository;
    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock
    private TournamentLikeRepository tournamentLikeRepository;

    private TournamentService tournamentService;
    private Clock clock;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.of(2026, 4, 18, 10, 15);
        clock = TestFixtures.fixedClock(now);
        tournamentService = new TournamentService(
                tournamentRepository,
                teamRepository,
                announcementRepository,
                scheduleEventRepository,
                taskRepository,
                submissionRepository,
                evaluationAssignmentRepository,
                evaluationRepository,
                tournamentLikeRepository,
                clock
        );
    }

    @Test
    void createTournamentRejectsInvalidTeamBounds() {
        TournamentRequest request = new TournamentRequest(
                "Spring Cup",
                "Tournament description",
                "Rules",
                now.plusDays(3),
                now.plusDays(1),
                now.plusDays(2),
                10,
                1,
                5,
                3,
                false
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> tournamentService.createTournament(request, TestFixtures.user(1L, "admin@example.com", UserRole.ADMIN))
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).contains("Team max members");
    }

    @Test
    void toResponseReflectsRegistrationWindowAndHiddenTeamsUsingClock() {
        UserEntity admin = TestFixtures.user(1L, "admin@example.com", UserRole.ADMIN);
        TournamentEntity tournament = TestFixtures.tournament(
                5L,
                TournamentStatus.REGISTRATION,
                now.minusHours(1),
                now.plusHours(2),
                true,
                admin
        );
        when(teamRepository.countByTournamentId(5L)).thenReturn(3L);
        when(tournamentLikeRepository.countByTournamentId(5L)).thenReturn(7L);
        when(tournamentLikeRepository.existsByTournamentIdAndUserId(5L, admin.getId())).thenReturn(true);

        TournamentResponse response = tournamentService.toResponse(tournament, admin);

        assertThat(response.registrationOpen()).isTrue();
        assertThat(response.teamsVisible()).isFalse();
        assertThat(response.registeredTeams()).isEqualTo(3L);
        assertThat(response.likeCount()).isEqualTo(7L);
        assertThat(response.likedByCurrentUser()).isTrue();
    }

    @Test
    void likeTournamentCreatesLikeOnceAndReturnsUpdatedState() {
        UserEntity user = TestFixtures.user(2L, "user@example.com", UserRole.USER);
        TournamentEntity tournament = TestFixtures.tournament(
                12L,
                TournamentStatus.REGISTRATION,
                now.minusHours(1),
                now.plusHours(2),
                false,
                TestFixtures.user(1L, "admin@example.com", UserRole.ADMIN)
        );

        when(tournamentRepository.findById(12L)).thenReturn(Optional.of(tournament));
        when(tournamentLikeRepository.existsByTournamentIdAndUserId(12L, 2L)).thenReturn(false, true);
        when(tournamentLikeRepository.countByTournamentId(12L)).thenReturn(1L);

        TournamentResponse response = tournamentService.likeTournament(12L, user);

        verify(tournamentLikeRepository).save(any(TournamentLikeEntity.class));
        assertThat(response.likeCount()).isEqualTo(1L);
        assertThat(response.likedByCurrentUser()).isTrue();
    }

    @Test
    void createAnnouncementUsesClockTimestamp() {
        UserEntity admin = TestFixtures.user(1L, "admin@example.com", UserRole.ADMIN);
        TournamentEntity tournament = TestFixtures.tournament(
                9L,
                TournamentStatus.RUNNING,
                now.minusDays(2),
                now.minusDays(1),
                false,
                admin
        );

        when(tournamentRepository.findById(9L)).thenReturn(Optional.of(tournament));
        when(announcementRepository.save(any(AnnouncementEntity.class))).thenAnswer(invocation -> {
            AnnouncementEntity entity = invocation.getArgument(0);
            entity.setId(11L);
            return entity;
        });

        var response = tournamentService.createAnnouncement(
                9L,
                new AnnouncementRequest("Start", "Round is live"),
                admin
        );

        assertThat(response.id()).isEqualTo(11L);
        assertThat(response.createdAt()).isEqualTo(now);
    }
}
