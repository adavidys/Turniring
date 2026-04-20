package com.example.turniring.submission.service;

import com.example.turniring.submission.dto.SubmissionResponse;
import com.example.turniring.submission.dto.UpsertSubmissionRequest;
import com.example.turniring.submission.entity.SubmissionEntity;
import com.example.turniring.submission.entity.SubmissionStatus;
import com.example.turniring.submission.repository.SubmissionRepository;
import com.example.turniring.task.entity.TaskEntity;
import com.example.turniring.task.service.TaskService;
import com.example.turniring.team.entity.TeamEntity;
import com.example.turniring.team.service.TeamService;
import com.example.turniring.user.entity.UserEntity;
import com.example.turniring.user.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final TaskService taskService;
    private final TeamService teamService;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    @Transactional
    public SubmissionResponse upsertSubmission(Long taskId, UpsertSubmissionRequest request) {
        UserEntity captain = currentUserService.requireCurrentUser();
        TaskEntity task = taskService.getTaskEntity(taskId);
        TeamEntity team = teamService.getCaptainTeamForTournament(task.getTournament().getId(), captain.getId());

        if (!taskService.isSubmissionOpen(task)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task submission window is closed");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        SubmissionEntity submission = submissionRepository.findByTaskIdAndTeamId(taskId, team.getId())
                .orElseGet(() -> SubmissionEntity.builder()
                        .task(task)
                        .team(team)
                        .submittedAt(now)
                        .build());

        submission.setGithubUrl(request.githubUrl());
        submission.setDemoVideoUrl(request.demoVideoUrl());
        submission.setLiveDemoUrl(request.liveDemoUrl());
        submission.setSummary(request.summary());
        submission.setUpdatedAt(now);
        submission.setStatus(SubmissionStatus.SUBMITTED);

        return SubmissionResponse.from(submissionRepository.save(submission));
    }

    @Transactional(readOnly = true)
    public SubmissionResponse getMySubmission(Long taskId) {
        UserEntity captain = currentUserService.requireCurrentUser();
        TaskEntity task = taskService.getTaskEntity(taskId);
        TeamEntity team = teamService.getCaptainTeamForTournament(task.getTournament().getId(), captain.getId());

        SubmissionEntity submission = submissionRepository.findByTaskIdAndTeamId(taskId, team.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));
        return SubmissionResponse.from(submission);
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> listTournamentSubmissions(Long tournamentId) {
        return submissionRepository.findAllByTaskTournamentIdOrderByUpdatedAtDesc(tournamentId).stream()
                .map(SubmissionResponse::from)
                .toList();
    }
}
