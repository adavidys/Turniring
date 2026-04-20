package com.example.turniring.evaluation.service;

import com.example.turniring.evaluation.dto.AssignEvaluationsRequest;
import com.example.turniring.evaluation.dto.EvaluationRequest;
import com.example.turniring.evaluation.dto.EvaluationResponse;
import com.example.turniring.evaluation.dto.JuryAssignmentResponse;
import com.example.turniring.evaluation.entity.EvaluationAssignmentEntity;
import com.example.turniring.evaluation.entity.EvaluationAssignmentStatus;
import com.example.turniring.evaluation.entity.EvaluationEntity;
import com.example.turniring.evaluation.repository.EvaluationAssignmentRepository;
import com.example.turniring.evaluation.repository.EvaluationRepository;
import com.example.turniring.submission.dto.SubmissionResponse;
import com.example.turniring.submission.entity.SubmissionEntity;
import com.example.turniring.submission.repository.SubmissionRepository;
import com.example.turniring.task.entity.TaskEntity;
import com.example.turniring.task.entity.TaskStatus;
import com.example.turniring.task.repository.TaskRepository;
import com.example.turniring.team.entity.TeamEntity;
import com.example.turniring.team.repository.TeamRepository;
import com.example.turniring.tournament.dto.LeaderboardEntryResponse;
import com.example.turniring.tournament.dto.LeaderboardResponse;
import com.example.turniring.tournament.dto.LeaderboardTaskScoreResponse;
import com.example.turniring.tournament.entity.TournamentEntity;
import com.example.turniring.tournament.repository.TournamentRepository;
import com.example.turniring.user.entity.UserEntity;
import com.example.turniring.user.entity.UserRole;
import com.example.turniring.user.service.CurrentUserService;
import com.example.turniring.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationAssignmentRepository evaluationAssignmentRepository;
    private final EvaluationRepository evaluationRepository;
    private final SubmissionRepository submissionRepository;
    private final TaskRepository taskRepository;
    private final TeamRepository teamRepository;
    private final TournamentRepository tournamentRepository;
    private final UserService userService;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    @Transactional
    public List<JuryAssignmentResponse> assignEvaluations(Long taskId, AssignEvaluationsRequest request) {
        TaskEntity task = getTask(taskId);
        if (task.getStatus() != TaskStatus.SUBMISSION_CLOSED && task.getStatus() != TaskStatus.EVALUATED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Assignments can be created only after submissions are closed"
            );
        }

        List<SubmissionEntity> submissions = submissionRepository.findAllByTaskIdOrderByUpdatedAtDesc(taskId);
        if (submissions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No submissions available for assignment");
        }

        List<UserEntity> juries = userService.getByRole(UserRole.JURY);
        if (juries.size() < request.evaluatorsPerSubmission()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough jury users to assign evaluations");
        }

        List<EvaluationAssignmentEntity> existingAssignments =
                evaluationAssignmentRepository.findAllBySubmissionTaskIdOrderByAssignedAtAsc(taskId);
        Map<Long, Integer> assignmentsPerJury = new HashMap<>();
        existingAssignments.forEach(assignment ->
                assignmentsPerJury.merge(assignment.getJury().getId(), 1, Integer::sum)
        );

        List<EvaluationAssignmentEntity> createdAssignments = new ArrayList<>();
        List<UserEntity> shuffledJuries = new ArrayList<>(juries);
        Collections.shuffle(shuffledJuries);

        for (SubmissionEntity submission : submissions) {
            Set<Long> assignedJuryIds = new HashSet<>();
            for (EvaluationAssignmentEntity assignment : existingAssignments) {
                if (assignment.getSubmission().getId().equals(submission.getId())) {
                    assignedJuryIds.add(assignment.getJury().getId());
                }
            }

            int missingAssignments = Math.max(0, request.evaluatorsPerSubmission() - assignedJuryIds.size());
            if (missingAssignments == 0) {
                continue;
            }

            List<UserEntity> candidates = shuffledJuries.stream()
                    .filter(jury -> !assignedJuryIds.contains(jury.getId()))
                    .filter(jury -> request.maxAssignmentsPerJury() == null
                            || assignmentsPerJury.getOrDefault(jury.getId(), 0) < request.maxAssignmentsPerJury())
                    .sorted(Comparator.comparingInt(jury -> assignmentsPerJury.getOrDefault(jury.getId(), 0)))
                    .toList();

            if (candidates.size() < missingAssignments) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Not enough available jury members to satisfy assignment constraints"
                );
            }

            for (int index = 0; index < missingAssignments; index++) {
                UserEntity jury = candidates.get(index);
                EvaluationAssignmentEntity assignment = EvaluationAssignmentEntity.builder()
                        .submission(submission)
                        .jury(jury)
                        .status(EvaluationAssignmentStatus.ASSIGNED)
                        .assignedAt(LocalDateTime.now(clock))
                        .build();

                createdAssignments.add(evaluationAssignmentRepository.save(assignment));
                assignmentsPerJury.merge(jury.getId(), 1, Integer::sum);
            }
        }

        return createdAssignments.stream()
                .map(assignment -> JuryAssignmentResponse.from(
                        assignment,
                        SubmissionResponse.from(assignment.getSubmission()),
                        null
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<JuryAssignmentResponse> getMyAssignments() {
        UserEntity jury = currentUserService.requireCurrentUser();
        return getAssignmentsForJury(jury.getId());
    }

    @Transactional(readOnly = true)
    public List<JuryAssignmentResponse> getAssignmentsForJury(Long juryId) {
        return evaluationAssignmentRepository.findAllByJuryIdOrderByAssignedAtDesc(juryId).stream()
                .map(assignment -> JuryAssignmentResponse.from(
                        assignment,
                        SubmissionResponse.from(assignment.getSubmission()),
                        evaluationRepository.findByAssignmentId(assignment.getId())
                                .map(EvaluationResponse::from)
                                .orElse(null)
                ))
                .toList();
    }

    @Transactional
    public EvaluationResponse submitEvaluation(Long assignmentId, EvaluationRequest request) {
        UserEntity jury = currentUserService.requireCurrentUser();
        EvaluationAssignmentEntity assignment = evaluationAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        if (!assignment.getJury().getId().equals(jury.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Assignment belongs to another jury member");
        }

        EvaluationEntity evaluation = evaluationRepository.findByAssignmentId(assignmentId)
                .orElseGet(() -> EvaluationEntity.builder().assignment(assignment).build());

        evaluation.setBackendScore(request.backendScore());
        evaluation.setDatabaseScore(request.databaseScore());
        evaluation.setFrontendScore(request.frontendScore());
        evaluation.setMustHaveScore(request.mustHaveScore());
        evaluation.setFunctionalityScore(request.functionalityScore());
        evaluation.setUsabilityScore(request.usabilityScore());
        evaluation.setComment(request.comment());
        evaluation.setTotalScore(calculateTotalScore(request));
        evaluation.setSubmittedAt(LocalDateTime.now(clock));

        assignment.setStatus(EvaluationAssignmentStatus.COMPLETED);
        evaluationAssignmentRepository.save(assignment);
        return EvaluationResponse.from(evaluationRepository.save(evaluation));
    }

    @Transactional
    public void finishEvaluation(Long taskId) {
        TaskEntity task = getTask(taskId);
        task.setStatus(TaskStatus.EVALUATED);
        taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public LeaderboardResponse getLeaderboard(Long tournamentId) {
        TournamentEntity tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournament not found"));

        List<TeamEntity> teams = teamRepository.findAllByTournamentIdOrderByCreatedAtAsc(tournamentId);
        List<TaskEntity> tasks = taskRepository.findAllByTournamentIdOrderByStartAtAsc(tournamentId);
        List<EvaluationEntity> evaluations = evaluationRepository.findAllByAssignmentSubmissionTaskTournamentId(tournamentId);
        List<SubmissionEntity> submissions = submissionRepository.findAllByTaskTournamentIdOrderByUpdatedAtDesc(tournamentId);

        Map<Long, Map<Long, SubmissionEntity>> submissionsByTeamAndTask = new HashMap<>();
        for (SubmissionEntity submission : submissions) {
            submissionsByTeamAndTask
                    .computeIfAbsent(submission.getTeam().getId(), ignored -> new HashMap<>())
                    .put(submission.getTask().getId(), submission);
        }

        Map<Long, List<Double>> evaluationTotalsBySubmission = new HashMap<>();
        for (EvaluationEntity evaluation : evaluations) {
            Long submissionId = evaluation.getAssignment().getSubmission().getId();
            evaluationTotalsBySubmission.computeIfAbsent(submissionId, ignored -> new ArrayList<>())
                    .add(evaluation.getTotalScore());
        }

        List<LeaderboardEntryResponse> entries = new ArrayList<>();
        for (TeamEntity team : teams) {
            List<LeaderboardTaskScoreResponse> taskScores = new ArrayList<>();
            double totalScore = 0.0;

            for (TaskEntity task : tasks) {
                SubmissionEntity submission = submissionsByTeamAndTask
                        .getOrDefault(team.getId(), Map.of())
                        .get(task.getId());

                double averageScore = 0.0;
                long evaluationCount = 0;
                if (submission != null) {
                    List<Double> scores = evaluationTotalsBySubmission.getOrDefault(submission.getId(), List.of());
                    evaluationCount = scores.size();
                    averageScore = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                }

                totalScore += averageScore;
                taskScores.add(new LeaderboardTaskScoreResponse(task.getId(), task.getTitle(), averageScore, evaluationCount));
            }

            entries.add(new LeaderboardEntryResponse(
                    0,
                    team.getId(),
                    team.getName(),
                    team.getCaptain().getEmail(),
                    totalScore,
                    taskScores
            ));
        }

        entries.sort(Comparator.comparingDouble(LeaderboardEntryResponse::totalScore).reversed());
        List<LeaderboardEntryResponse> rankedEntries = new ArrayList<>();
        for (int index = 0; index < entries.size(); index++) {
            LeaderboardEntryResponse entry = entries.get(index);
            rankedEntries.add(new LeaderboardEntryResponse(
                    index + 1,
                    entry.teamId(),
                    entry.teamName(),
                    entry.captainEmail(),
                    entry.totalScore(),
                    entry.taskScores()
            ));
        }

        return new LeaderboardResponse(
                tournament.getId(),
                tournament.getTitle(),
                "Each task score is the average of all jury totals for that submission. Tournament score is the sum of task averages.",
                rankedEntries
        );
    }

    @Transactional(readOnly = true)
    public String exportLeaderboardCsv(Long tournamentId) {
        LeaderboardResponse leaderboard = getLeaderboard(tournamentId);
        StringBuilder csv = new StringBuilder();
        csv.append("Position,Team,Captain Email,Total Score\n");
        for (LeaderboardEntryResponse entry : leaderboard.entries()) {
            csv.append(entry.position()).append(",")
                    .append(escape(entry.teamName())).append(",")
                    .append(escape(entry.captainEmail())).append(",")
                    .append(entry.totalScore()).append("\n");
        }
        return csv.toString();
    }

    private TaskEntity getTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    private double calculateTotalScore(EvaluationRequest request) {
        return (
                request.backendScore()
                        + request.databaseScore()
                        + request.frontendScore()
                        + request.mustHaveScore()
                        + request.functionalityScore()
                        + request.usabilityScore()
        ) / 6.0;
    }

    private String escape(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
