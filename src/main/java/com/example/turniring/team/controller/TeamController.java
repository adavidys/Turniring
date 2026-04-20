package com.example.turniring.team.controller;

import com.example.turniring.task.dto.TaskResponse;
import com.example.turniring.task.service.TaskService;
import com.example.turniring.team.dto.TeamCreateRequest;
import com.example.turniring.team.dto.TeamRegistrationRequest;
import com.example.turniring.team.dto.TeamResponse;
import com.example.turniring.team.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/team")
@SecurityRequirement(name = "bearerAuth")
public class TeamController {

    private final TeamService teamService;
    private final TaskService taskService;

    @PostMapping("/teams")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a team")
    public TeamResponse createTeam(
            @Valid @RequestBody TeamCreateRequest request
    ) {
        return teamService.createTeam(request);
    }

    @PostMapping("/teams/{teamId}/join/{tournamentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Join olympiad with existing team")
    public TeamResponse joinTeam(@PathVariable Long teamId, @PathVariable Long tournamentId) {
        return teamService.joinTeam(teamId, tournamentId);
    }

    @PostMapping("/teams/{teamId}/leave")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Leave olympiad with team")
    public TeamResponse leaveTeam(@PathVariable Long teamId) {
        return teamService.leaveTournament(teamId);
    }

    @DeleteMapping("/teams/{teamId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete team")
    public ResponseEntity<Void> deleteTeam(@PathVariable Long teamId) {
        teamService.deleteTeam(teamId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/teams/{teamId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update team roster")
    public TeamResponse updateTeam(
            @PathVariable Long teamId,
            @Valid @RequestBody TeamRegistrationRequest request
    ) {
        return teamService.updateTeam(teamId, request);
    }

    @GetMapping("/teams/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get teams owned by current captain")
    public List<TeamResponse> myTeams() {
        return teamService.listMyTeams();
    }

    @GetMapping("/tournaments/{tournamentId}/tasks")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get tournament tasks for team workspace")
    public List<TaskResponse> tournamentTasks(@PathVariable Long tournamentId) {
        return taskService.listVisibleByTournament(tournamentId);
    }
}
