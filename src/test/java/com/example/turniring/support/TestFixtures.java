package com.example.turniring.support;

import com.example.turniring.evaluation.entity.EvaluationAssignmentEntity;
import com.example.turniring.evaluation.entity.EvaluationAssignmentStatus;
import com.example.turniring.evaluation.entity.EvaluationEntity;
import com.example.turniring.submission.entity.SubmissionEntity;
import com.example.turniring.submission.entity.SubmissionStatus;
import com.example.turniring.task.entity.TaskEntity;
import com.example.turniring.task.entity.TaskStatus;
import com.example.turniring.team.entity.TeamEntity;
import com.example.turniring.tournament.entity.TournamentEntity;
import com.example.turniring.tournament.entity.TournamentStatus;
import com.example.turniring.user.entity.UserEntity;
import com.example.turniring.user.entity.UserRole;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static Clock fixedClock(LocalDateTime dateTime) {
        return Clock.fixed(dateTime.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }

    public static UserEntity user(Long id, String email, UserRole role) {
        return UserEntity.builder()
                .id(id)
                .name("Test")
                .lastName("User")
                .email(email)
                .password("encoded-password")
                .role(role)
                .build();
    }

    public static TournamentEntity tournament(
            Long id,
            TournamentStatus status,
            LocalDateTime registrationStart,
            LocalDateTime registrationEnd,
            boolean hideTeamsUntilRegistrationEnds,
            UserEntity createdBy
    ) {
        return TournamentEntity.builder()
                .id(id)
                .title("Tournament " + id)
                .description("Description")
                .rules("Rules")
                .startAt(registrationEnd.plusDays(1))
                .registrationStartAt(registrationStart)
                .registrationEndAt(registrationEnd)
                .maxTeams(10)
                .minimumRounds(1)
                .teamMinMembers(2)
                .teamMaxMembers(5)
                .hideTeamsUntilRegistrationEnds(hideTeamsUntilRegistrationEnds)
                .status(status)
                .createdBy(createdBy)
                .build();
    }

    public static TeamEntity team(Long id, String name, TournamentEntity tournament, UserEntity captain, LocalDateTime createdAt) {
        return TeamEntity.builder()
                .id(id)
                .name(name)
                .tournament(tournament)
                .captain(captain)
                .city("Kyiv")
                .organization("School")
                .contactHandle("@team")
                .createdAt(createdAt)
                .build();
    }

    public static TaskEntity task(
            Long id,
            TournamentEntity tournament,
            TaskStatus status,
            LocalDateTime startAt,
            LocalDateTime deadlineAt
    ) {
        return TaskEntity.builder()
                .id(id)
                .tournament(tournament)
                .title("Task " + id)
                .description("Build feature")
                .technologyRequirements("Java")
                .mustHaveCriteria("JWT\nTests")
                .additionalMaterialsUrl("https://example.com/materials")
                .startAt(startAt)
                .deadlineAt(deadlineAt)
                .status(status)
                .build();
    }

    public static SubmissionEntity submission(Long id, TaskEntity task, TeamEntity team, LocalDateTime submittedAt, double suffix) {
        return SubmissionEntity.builder()
                .id(id)
                .task(task)
                .team(team)
                .githubUrl("https://github.com/example/repo-" + (int) suffix)
                .demoVideoUrl("https://youtu.be/demo-" + (int) suffix)
                .liveDemoUrl("https://demo.example.com/" + (int) suffix)
                .summary("Summary " + suffix)
                .submittedAt(submittedAt)
                .updatedAt(submittedAt)
                .status(SubmissionStatus.SUBMITTED)
                .build();
    }

    public static EvaluationAssignmentEntity assignment(
            Long id,
            SubmissionEntity submission,
            UserEntity jury,
            EvaluationAssignmentStatus status,
            LocalDateTime assignedAt
    ) {
        return EvaluationAssignmentEntity.builder()
                .id(id)
                .submission(submission)
                .jury(jury)
                .status(status)
                .assignedAt(assignedAt)
                .build();
    }

    public static EvaluationEntity evaluation(Long id, EvaluationAssignmentEntity assignment, double totalScore, LocalDateTime submittedAt) {
        return EvaluationEntity.builder()
                .id(id)
                .assignment(assignment)
                .backendScore(80)
                .databaseScore(80)
                .frontendScore(80)
                .mustHaveScore(80)
                .functionalityScore(80)
                .usabilityScore(80)
                .comment("Looks good")
                .totalScore(totalScore)
                .submittedAt(submittedAt)
                .build();
    }
}
