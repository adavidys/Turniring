package com.example.turniring.tournament.controller;

import com.example.turniring.evaluation.service.EvaluationService;
import com.example.turniring.task.dto.TaskResponse;
import com.example.turniring.task.service.TaskService;
import com.example.turniring.team.dto.TeamResponse;
import com.example.turniring.team.service.TeamService;
import com.example.turniring.tournament.dto.*;
import com.example.turniring.tournament.entity.TournamentStatus;
import com.example.turniring.tournament.service.TournamentService;
import com.example.turniring.user.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public")
public class PublicTournamentController {

    private final TournamentService tournamentService;
    private final TeamService teamService;
    private final TaskService taskService;
    private final EvaluationService evaluationService;
    private final CurrentUserService currentUserService;

    @GetMapping("/home")
    @Operation(summary = "Get grouped public homepage data")
    public HomeResponse home() {
        return tournamentService.buildHomeResponse(currentUserService.getCurrentUser().orElse(null));
    }

    @GetMapping("/tournaments")
    @Operation(summary = "List tournaments with optional status filter")
    public List<TournamentResponse> tournaments(@RequestParam(required = false) TournamentStatus status) {
        return tournamentService.listTournaments(status, currentUserService.getCurrentUser().orElse(null));
    }

    @GetMapping("/tournaments/recommended")
    @Operation(summary = "List recommended tournaments")
    public List<TournamentResponse> recommendedTournaments() {
        return tournamentService.recommendTournaments(currentUserService.getCurrentUser().orElse(null));
    }

    @GetMapping("/tournaments/{tournamentId}")
    @Operation(summary = "Get tournament details")
    public TournamentResponse tournament(@PathVariable Long tournamentId) {
        return tournamentService.getTournament(tournamentId, currentUserService.getCurrentUser().orElse(null));
    }

    @PostMapping("/tournaments/{tournamentId}/like")
    @Operation(summary = "Like tournament")
    public TournamentResponse likeTournament(@PathVariable Long tournamentId) {
        return tournamentService.likeTournament(tournamentId, currentUserService.requireCurrentUser());
    }

    @DeleteMapping("/tournaments/{tournamentId}/like")
    @Operation(summary = "Remove tournament like")
    public TournamentResponse unlikeTournament(@PathVariable Long tournamentId) {
        return tournamentService.unlikeTournament(tournamentId, currentUserService.requireCurrentUser());
    }

    @GetMapping("/tournaments/{tournamentId}/teams")
    @Operation(summary = "Get public tournament teams")
    public List<TeamResponse> teams(@PathVariable Long tournamentId) {
        return teamService.listTeamsForTournament(tournamentId);
    }

    @GetMapping("/tournaments/{tournamentId}/tasks")
    @Operation(summary = "Get tournament tasks")
    public List<TaskResponse> tasks(@PathVariable Long tournamentId) {
        return taskService.listVisibleByTournament(tournamentId);
    }

    @GetMapping("/tournaments/{tournamentId}/announcements")
    @Operation(summary = "Get tournament announcements")
    public List<AnnouncementResponse> announcements(@PathVariable Long tournamentId) {
        return tournamentService.listAnnouncements(tournamentId);
    }

    @GetMapping("/tournaments/{tournamentId}/schedule")
    @Operation(summary = "Get tournament schedule")
    public List<ScheduleEventResponse> schedule(@PathVariable Long tournamentId) {
        return tournamentService.listScheduleEvents(tournamentId);
    }

    @GetMapping("/tournaments/{tournamentId}/leaderboard")
    @Operation(summary = "Get tournament leaderboard")
    public LeaderboardResponse leaderboard(@PathVariable Long tournamentId) {
        return evaluationService.getLeaderboard(tournamentId);
    }
}
