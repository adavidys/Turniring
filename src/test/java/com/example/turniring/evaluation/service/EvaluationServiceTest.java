package com.example.turniring.evaluation.service;

import com.example.turniring.evaluation.dto.AssignEvaluationsRequest;
import com.example.turniring.evaluation.dto.EvaluationRequest;
import com.example.turniring.evaluation.entity.EvaluationAssignmentEntity;
import com.example.turniring.evaluation.entity.EvaluationAssignmentStatus;
import com.example.turniring.evaluation.entity.EvaluationEntity;
import com.example.turniring.evaluation.repository.EvaluationAssignmentRepository;
import com.example.turniring.evaluation.repository.EvaluationRepository;
import com.example.turniring.submission.entity.SubmissionEntity;
import com.example.turniring.submission.repository.SubmissionRepository;
import com.example.turniring.support.TestFixtures;
import com.example.turniring.task.entity.TaskEntity;
import com.example.turniring.task.entity.TaskStatus;
import com.example.turniring.task.repository.TaskRepository;
import com.example.turniring.team.entity.TeamEntity;
import com.example.turniring.team.repository.TeamRepository;
import com.example.turniring.tournament.entity.TournamentEntity;
import com.example.turniring.tournament.entity.TournamentStatus;
import com.example.turniring.tournament.repository.TournamentRepository;
import com.example.turniring.user.entity.UserEntity;
import com.example.turniring.user.entity.UserRole;
import com.example.turniring.user.service.CurrentUserService;
import com.example.turniring.user.service.UserService;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock
    private EvaluationAssignmentRepository evaluationAssignmentRepository;
    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private UserService userService;
    @Mock
    private CurrentUserService currentUserService;

    private EvaluationService evaluationService;
    private LocalDateTime now;
    private TournamentEntity tournament;
    private TaskEntity closedTask;
    private UserEntity juryOne;
    private UserEntity juryTwo;
    private TeamEntity teamOne;
    private TeamEntity teamTwo;
    private SubmissionEntity submissionOne;
    private SubmissionEntity submissionTwo;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.of(2026, 4, 18, 18, 0);
        Clock clock = TestFixtures.fixedClock(now);
        evaluationService = new EvaluationService(
                evaluationAssignmentRepository,
                evaluationRepository,
                submissionRepository,
                taskRepository,
                teamRepository,
                tournamentRepository,
                userService,
                currentUserService,
                clock
        );

        UserEntity admin = TestFixtures.user(1L, "admin@example.com", UserRole.ADMIN);
        tournament = TestFixtures.tournament(100L, TournamentStatus.RUNNING, now.minusDays(3), now.minusDays(2), false, admin);
        closedTask = TestFixtures.task(200L, tournament, TaskStatus.SUBMISSION_CLOSED, now.minusDays(1), now.minusHours(1));
        juryOne = TestFixtures.user(10L, "jury1@example.com", UserRole.JURY);
        juryTwo = TestFixtures.user(11L, "jury2@example.com", UserRole.JURY);
        UserEntity captainOne = TestFixtures.user(21L, "captain1@example.com", UserRole.TEAM);
        UserEntity captainTwo = TestFixtures.user(22L, "captain2@example.com", UserRole.TEAM);
        teamOne = TestFixtures.team(31L, "Alpha", tournament, captainOne, now.minusDays(2));
        teamTwo = TestFixtures.team(32L, "Beta", tournament, captainTwo, now.minusDays(2));
        submissionOne = TestFixtures.submission(41L, closedTask, teamOne, now.minusHours(3), 1);
        submissionTwo = TestFixtures.submission(42L, closedTask, teamTwo, now.minusHours(3), 2);
    }

    @Test
    void assignEvaluationsRejectsWhenTaskStillActive() {
        TaskEntity activeTask = TestFixtures.task(201L, tournament, TaskStatus.ACTIVE, now.minusHours(1), now.plusHours(1));
        when(taskRepository.findById(201L)).thenReturn(Optional.of(activeTask));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> evaluationService.assignEvaluations(201L, new AssignEvaluationsRequest(1, 2))
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).contains("submissions are closed");
    }

    @Test
    void assignEvaluationsCreatesAssignmentsForSubmissions() {
        when(taskRepository.findById(200L)).thenReturn(Optional.of(closedTask));
        when(submissionRepository.findAllByTaskIdOrderByUpdatedAtDesc(200L)).thenReturn(List.of(submissionOne, submissionTwo));
        when(userService.getByRole(UserRole.JURY)).thenReturn(List.of(juryOne, juryTwo));
        when(evaluationAssignmentRepository.findAllBySubmissionTaskIdOrderByAssignedAtAsc(200L)).thenReturn(List.of());
        when(evaluationAssignmentRepository.save(any(EvaluationAssignmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var assignments = evaluationService.assignEvaluations(200L, new AssignEvaluationsRequest(1, 1));

        assertThat(assignments).hasSize(2);
        verify(evaluationAssignmentRepository, times(2)).save(argThat(assignment ->
                assignment.getStatus() == EvaluationAssignmentStatus.ASSIGNED
                        && assignment.getAssignedAt().equals(now)
        ));
    }

    @Test
    void submitEvaluationMarksAssignmentCompletedAndComputesAverage() {
        EvaluationAssignmentEntity assignment = TestFixtures.assignment(
                77L,
                submissionOne,
                juryOne,
                EvaluationAssignmentStatus.ASSIGNED,
                now.minusHours(1)
        );
        when(currentUserService.requireCurrentUser()).thenReturn(juryOne);
        when(evaluationAssignmentRepository.findById(77L)).thenReturn(Optional.of(assignment));
        when(evaluationRepository.findByAssignmentId(77L)).thenReturn(Optional.empty());
        when(evaluationRepository.save(any(EvaluationEntity.class))).thenAnswer(invocation -> {
            EvaluationEntity evaluation = invocation.getArgument(0);
            evaluation.setId(88L);
            return evaluation;
        });

        var response = evaluationService.submitEvaluation(
                77L,
                new EvaluationRequest(90, 80, 70, 100, 80, 60, "Strong work")
        );

        ArgumentCaptor<EvaluationEntity> captor = ArgumentCaptor.forClass(EvaluationEntity.class);
        verify(evaluationRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalScore()).isEqualTo(80.0);
        assertThat(captor.getValue().getSubmittedAt()).isEqualTo(now);
        assertThat(assignment.getStatus()).isEqualTo(EvaluationAssignmentStatus.COMPLETED);
        assertThat(response.id()).isEqualTo(88L);
    }

    @Test
    void getLeaderboardRanksTeamsByAverageScores() {
        EvaluationAssignmentEntity assignmentOne = TestFixtures.assignment(1L, submissionOne, juryOne, EvaluationAssignmentStatus.COMPLETED, now.minusHours(2));
        EvaluationAssignmentEntity assignmentTwo = TestFixtures.assignment(2L, submissionOne, juryTwo, EvaluationAssignmentStatus.COMPLETED, now.minusHours(2));
        EvaluationAssignmentEntity assignmentThree = TestFixtures.assignment(3L, submissionTwo, juryOne, EvaluationAssignmentStatus.COMPLETED, now.minusHours(2));

        when(tournamentRepository.findById(100L)).thenReturn(Optional.of(tournament));
        when(teamRepository.findAllByTournamentIdOrderByCreatedAtAsc(100L)).thenReturn(List.of(teamOne, teamTwo));
        when(taskRepository.findAllByTournamentIdOrderByStartAtAsc(100L)).thenReturn(List.of(closedTask));
        when(submissionRepository.findAllByTaskTournamentIdOrderByUpdatedAtDesc(100L)).thenReturn(List.of(submissionOne, submissionTwo));
        when(evaluationRepository.findAllByAssignmentSubmissionTaskTournamentId(100L)).thenReturn(List.of(
                TestFixtures.evaluation(11L, assignmentOne, 80.0, now.minusHours(1)),
                TestFixtures.evaluation(12L, assignmentTwo, 100.0, now.minusHours(1)),
                TestFixtures.evaluation(13L, assignmentThree, 70.0, now.minusHours(1))
        ));

        var leaderboard = evaluationService.getLeaderboard(100L);

        assertThat(leaderboard.entries()).hasSize(2);
        assertThat(leaderboard.entries().get(0).teamName()).isEqualTo("Alpha");
        assertThat(leaderboard.entries().get(0).totalScore()).isEqualTo(90.0);
        assertThat(leaderboard.entries().get(0).position()).isEqualTo(1);
        assertThat(leaderboard.entries().get(1).teamName()).isEqualTo("Beta");
        assertThat(leaderboard.entries().get(1).totalScore()).isEqualTo(70.0);
    }
}
