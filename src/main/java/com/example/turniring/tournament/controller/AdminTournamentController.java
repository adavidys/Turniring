package com.example.turniring.tournament.controller;

import com.example.turniring.evaluation.dto.AssignEvaluationsRequest;
import com.example.turniring.evaluation.dto.JuryAssignmentResponse;
import com.example.turniring.evaluation.service.EvaluationService;
import com.example.turniring.submission.dto.SubmissionResponse;
import com.example.turniring.submission.service.SubmissionService;
import com.example.turniring.task.dto.TaskRequest;
import com.example.turniring.task.dto.TaskResponse;
import com.example.turniring.task.entity.TaskStatus;
import com.example.turniring.task.service.TaskService;
import com.example.turniring.team.dto.TeamResponse;
import com.example.turniring.team.service.TeamService;
import com.example.turniring.tournament.dto.*;
import com.example.turniring.tournament.entity.TournamentStatus;
import com.example.turniring.tournament.service.TournamentService;
import com.example.turniring.user.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
public class AdminTournamentController {

    private final TournamentService tournamentService;
    private final TaskService taskService;
    private final TeamService teamService;
    private final SubmissionService submissionService;
    private final EvaluationService evaluationService;
    private final CurrentUserService currentUserService;

    @PostMapping("/tournaments")
    @Operation(summary = "Create tournament")
    public TournamentResponse createTournament(@Valid @RequestBody TournamentRequest request) {
        return tournamentService.createTournament(request, currentUserService.requireCurrentUser());
    }

    @PutMapping("/tournaments/{tournamentId}")
    @Operation(summary = "Update tournament")
    public TournamentResponse updateTournament(@PathVariable Long tournamentId, @Valid @RequestBody TournamentRequest request) {
        return tournamentService.updateTournament(tournamentId, request);
    }

    @DeleteMapping("/tournaments/{tournamentId}")
    @Operation(summary = "Delete tournament")
    public ResponseEntity<Void> deleteTournament(
            @PathVariable Long tournamentId,
            @Valid @RequestBody TournamentDeleteRequest request
    ) {
        tournamentService.deleteTournament(tournamentId, request.confirmationText());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tournaments/{tournamentId}/status/{status}")
    @Operation(summary = "Update tournament status")
    public TournamentResponse updateTournamentStatus(@PathVariable Long tournamentId, @PathVariable TournamentStatus status) {
        return tournamentService.updateStatus(tournamentId, status);
    }

    @PostMapping("/tournaments/{tournamentId}/announcements")
    @Operation(summary = "Create tournament announcement")
    public AnnouncementResponse createAnnouncement(
            @PathVariable Long tournamentId,
            @Valid @RequestBody AnnouncementRequest request
    ) {
        return tournamentService.createAnnouncement(tournamentId, request, currentUserService.requireCurrentUser());
    }

    @PostMapping("/tournaments/{tournamentId}/schedule")
    @Operation(summary = "Create schedule event")
    public ScheduleEventResponse createScheduleEvent(
            @PathVariable Long tournamentId,
            @Valid @RequestBody ScheduleEventRequest request
    ) {
        return tournamentService.createScheduleEvent(tournamentId, request);
    }

    @PostMapping("/tournaments/{tournamentId}/tasks")
    @Operation(summary = "Create task for tournament")
    public TaskResponse createTask(@PathVariable Long tournamentId, @Valid @RequestBody TaskRequest request) {
        return taskService.createTask(tournamentId, request);
    }

    @GetMapping("/tournaments/{tournamentId}/teams")
    @Operation(summary = "List tournament teams for administration")
    public List<TeamResponse> teams(@PathVariable Long tournamentId) {
        return teamService.listTeamsForTournamentAdmin(tournamentId);
    }

    @PostMapping("/tasks/{taskId}/status/{status}")
    @Operation(summary = "Update task status")
    public TaskResponse updateTaskStatus(@PathVariable Long taskId, @PathVariable TaskStatus status) {
        return taskService.updateStatus(taskId, status);
    }

    @GetMapping("/tournaments/{tournamentId}/submissions")
    @Operation(summary = "List tournament submissions")
    public List<SubmissionResponse> submissions(@PathVariable Long tournamentId) {
        return submissionService.listTournamentSubmissions(tournamentId);
    }

    @PostMapping("/tasks/{taskId}/assignments")
    @Operation(summary = "Assign jury evaluations to submissions")
    public List<JuryAssignmentResponse> assignEvaluations(
            @PathVariable Long taskId,
            @Valid @RequestBody AssignEvaluationsRequest request
    ) {
        return evaluationService.assignEvaluations(taskId, request);
    }

    @PostMapping("/tasks/{taskId}/finish-evaluation")
    @Operation(summary = "Force finish evaluation for task")
    public ResponseEntity<Void> finishEvaluation(@PathVariable Long taskId) {
        evaluationService.finishEvaluation(taskId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tournaments/{tournamentId}/leaderboard/export")
    @Operation(summary = "Export tournament leaderboard to CSV")
    public ResponseEntity<String> exportLeaderboard(@PathVariable Long tournamentId) {
        String csv = evaluationService.exportLeaderboardCsv(tournamentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tournament-" + tournamentId + "-leaderboard.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
