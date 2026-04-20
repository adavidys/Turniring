package com.example.turniring.evaluation.controller;

import com.example.turniring.evaluation.dto.EvaluationRequest;
import com.example.turniring.evaluation.dto.EvaluationResponse;
import com.example.turniring.evaluation.dto.JuryAssignmentResponse;
import com.example.turniring.evaluation.service.EvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/jury")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('JURY')")
public class JuryController {

    private final EvaluationService evaluationService;

    @GetMapping("/assignments")
    @Operation(summary = "Get assignments for current jury member")
    public List<JuryAssignmentResponse> assignments() {
        return evaluationService.getMyAssignments();
    }

    @PostMapping("/assignments/{assignmentId}/evaluation")
    @Operation(summary = "Submit evaluation for assigned submission")
    public EvaluationResponse submitEvaluation(
            @PathVariable Long assignmentId,
            @Valid @RequestBody EvaluationRequest request
    ) {
        return evaluationService.submitEvaluation(assignmentId, request);
    }
}
