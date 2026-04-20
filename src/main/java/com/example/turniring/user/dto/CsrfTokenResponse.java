package com.example.turniring.user.dto;

public record CsrfTokenResponse(
        String token,
        String headerName,
        String parameterName
) {
}
