package com.example.turniring.task.service;

import com.example.turniring.task.dto.TaskRequest;
import com.example.turniring.task.dto.TaskResponse;
import com.example.turniring.task.entity.TaskEntity;
import com.example.turniring.task.entity.TaskStatus;
import com.example.turniring.task.repository.TaskRepository;
import com.example.turniring.tournament.entity.TournamentEntity;
import com.example.turniring.tournament.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TournamentService tournamentService;
    private final Clock clock;

    @Transactional
    public TaskResponse createTask(Long tournamentId, TaskRequest request) {
        validateTaskRequest(request);

        TournamentEntity tournament = tournamentService.getTournamentEntity(tournamentId);
        TaskEntity task = TaskEntity.builder()
                .tournament(tournament)
                .title(request.title())
                .description(request.description())
                .technologyRequirements(request.technologyRequirements())
                .mustHaveCriteria(joinLines(request.mustHaveCriteria()))
                .additionalMaterialsUrl(request.additionalMaterialsUrl())
                .startAt(request.startAt())
                .deadlineAt(request.deadlineAt())
                .status(TaskStatus.DRAFT)
                .build();

        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse updateStatus(Long taskId, TaskStatus status) {
        TaskEntity task = getTaskEntity(taskId);
        task.setStatus(status);
        return toResponse(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public TaskEntity getTaskEntity(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listByTournament(Long tournamentId) {
        tournamentService.getTournamentEntity(tournamentId);
        return taskRepository.findAllByTournamentIdOrderByStartAtAsc(tournamentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listVisibleByTournament(Long tournamentId) {
        return listByTournament(tournamentId).stream()
                .filter(task -> task.status() != TaskStatus.DRAFT)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(Long taskId) {
        return toResponse(getTaskEntity(taskId));
    }

    public boolean isSubmissionOpen(TaskEntity task) {
        LocalDateTime now = LocalDateTime.now(clock);
        return task.getStatus() == TaskStatus.ACTIVE
                && !now.isBefore(task.getStartAt())
                && !now.isAfter(task.getDeadlineAt());
    }

    public TaskResponse toResponse(TaskEntity task) {
        return TaskResponse.from(task, splitLines(task.getMustHaveCriteria()));
    }

    private void validateTaskRequest(TaskRequest request) {
        if (request.deadlineAt().isBefore(request.startAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task deadline must be after start");
        }
    }

    private String joinLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return null;
        }
        return String.join("\n", lines);
    }

    private List<String> splitLines(String lines) {
        if (lines == null || lines.isBlank()) {
            return Collections.emptyList();
        }
        return List.of(lines.split("\\n"));
    }
}
