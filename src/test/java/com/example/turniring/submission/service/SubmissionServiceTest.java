package com.example.turniring.submission.service;

import com.example.turniring.submission.dto.UpsertSubmissionRequest;
import com.example.turniring.submission.entity.SubmissionEntity;
import com.example.turniring.submission.repository.SubmissionRepository;
import com.example.turniring.support.TestFixtures;
import com.example.turniring.task.entity.TaskEntity;
import com.example.turniring.task.entity.TaskStatus;
import com.example.turniring.task.service.TaskService;
import com.example.turniring.team.entity.TeamEntity;
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
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private TaskService taskService;
    @Mock
    private TeamService teamService;
    @Mock
    private CurrentUserService currentUserService;

    private SubmissionService submissionService;
    private LocalDateTime now;
    private TaskEntity task;
    private TeamEntity team;
    private UserEntity captain;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.of(2026, 4, 18, 16, 45);
        Clock clock = TestFixtures.fixedClock(now);
        submissionService = new SubmissionService(submissionRepository, taskService, teamService, currentUserService, clock);

        captain = TestFixtures.user(5L, "captain@example.com", UserRole.TEAM);
        TournamentEntity tournament = TestFixtures.tournament(
                9L,
                TournamentStatus.RUNNING,
                now.minusDays(2),
                now.minusDays(1),
                false,
                TestFixtures.user(1L, "admin@example.com", UserRole.ADMIN)
        );
        task = TestFixtures.task(15L, tournament, TaskStatus.ACTIVE, now.minusHours(1), now.plusHours(1));
        team = TestFixtures.team(20L, "Code Masters", tournament, captain, now.minusDays(1));
    }

    @Test
    void upsertSubmissionCreatesNewSubmissionWithFixedTimestamps() {
        UpsertSubmissionRequest request = new UpsertSubmissionRequest(
                "https://github.com/example/repo",
                "https://youtu.be/demo",
                "https://demo.example.com",
                "Implemented all must-have features"
        );

        when(currentUserService.requireCurrentUser()).thenReturn(captain);
        when(taskService.getTaskEntity(15L)).thenReturn(task);
        when(taskService.isSubmissionOpen(task)).thenReturn(true);
        when(teamService.getCaptainTeamForTournament(9L, 5L)).thenReturn(team);
        when(submissionRepository.findByTaskIdAndTeamId(15L, 20L)).thenReturn(Optional.empty());
        when(submissionRepository.save(any(SubmissionEntity.class))).thenAnswer(invocation -> {
            SubmissionEntity submission = invocation.getArgument(0);
            submission.setId(101L);
            return submission;
        });

        var response = submissionService.upsertSubmission(15L, request);

        ArgumentCaptor<SubmissionEntity> captor = ArgumentCaptor.forClass(SubmissionEntity.class);
        verify(submissionRepository).save(captor.capture());
        SubmissionEntity saved = captor.getValue();
        assertThat(saved.getSubmittedAt()).isEqualTo(now);
        assertThat(saved.getUpdatedAt()).isEqualTo(now);
        assertThat(response.id()).isEqualTo(101L);
    }

    @Test
    void upsertSubmissionRejectsClosedWindow() {
        when(currentUserService.requireCurrentUser()).thenReturn(captain);
        when(taskService.getTaskEntity(15L)).thenReturn(task);
        when(teamService.getCaptainTeamForTournament(9L, 5L)).thenReturn(team);
        when(taskService.isSubmissionOpen(task)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> submissionService.upsertSubmission(
                        15L,
                        new UpsertSubmissionRequest("repo", "video", null, null)
                )
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(submissionRepository, never()).save(any());
    }
}
