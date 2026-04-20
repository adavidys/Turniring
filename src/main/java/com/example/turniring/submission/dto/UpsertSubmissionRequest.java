package com.example.turniring.submission.dto;

import jakarta.validation.constraints.NotBlank;

public record UpsertSubmissionRequest(
        @NotBlank(message = "GitHub URL is required")
        String githubUrl,

        @NotBlank(message = "Demo video URL is required")
        String demoVideoUrl,

        String liveDemoUrl,

        String summary
) {
}
