package com.example.turniring.submission.controller;

import com.example.turniring.submission.dto.SubmissionResponse;
import com.example.turniring.submission.dto.UpsertSubmissionRequest;
import com.example.turniring.submission.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/team/tasks")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
public class SubmissionController {

    private final SubmissionService submissionService;

    @PutMapping("/{taskId}/submission")
    @Operation(summary = "Create or update task submission")
    public SubmissionResponse upsertSubmission(
            @PathVariable Long taskId,
            @Valid @RequestBody UpsertSubmissionRequest request
    ) {
        return submissionService.upsertSubmission(taskId, request);
    }

    @GetMapping("/{taskId}/submission")
    @Operation(summary = "Get current team submission for task")
    public SubmissionResponse getSubmission(@PathVariable Long taskId) {
        return submissionService.getMySubmission(taskId);
    }
}
