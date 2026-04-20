package com.example.turniring.task.service;

import com.example.turniring.support.TestFixtures;
import com.example.turniring.task.entity.TaskEntity;
import com.example.turniring.task.entity.TaskStatus;
import com.example.turniring.task.repository.TaskRepository;
import com.example.turniring.tournament.entity.TournamentEntity;
import com.example.turniring.tournament.service.TournamentService;
import com.example.turniring.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TournamentService tournamentService;

    private TaskService taskService;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.of(2026, 4, 18, 12, 0);
        Clock clock = TestFixtures.fixedClock(now);
        taskService = new TaskService(taskRepository, tournamentService, clock);
    }

    @Test
    void isSubmissionOpenUsesClockWindow() {
        TournamentEntity tournament = TestFixtures.tournament(
                1L,
                com.example.turniring.tournament.entity.TournamentStatus.RUNNING,
                now.minusDays(2),
                now.minusDays(1),
                false,
                TestFixtures.user(1L, "admin@example.com", UserRole.ADMIN)
        );
        TaskEntity task = TestFixtures.task(10L, tournament, TaskStatus.ACTIVE, now.minusHours(1), now.plusHours(1));

        assertThat(taskService.isSubmissionOpen(task)).isTrue();
    }

    @Test
    void isSubmissionOpenReturnsFalseOutsideWindow() {
        TournamentEntity tournament = TestFixtures.tournament(
                1L,
                com.example.turniring.tournament.entity.TournamentStatus.RUNNING,
                now.minusDays(2),
                now.minusDays(1),
                false,
                TestFixtures.user(1L, "admin@example.com", UserRole.ADMIN)
        );
        TaskEntity task = TestFixtures.task(10L, tournament, TaskStatus.ACTIVE, now.plusHours(1), now.plusHours(2));

        assertThat(taskService.isSubmissionOpen(task)).isFalse();
    }
}
