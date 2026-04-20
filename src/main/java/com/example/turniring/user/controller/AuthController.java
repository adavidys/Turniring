package com.example.turniring.user.controller;

import com.example.turniring.user.dto.AuthResponse;
import com.example.turniring.user.dto.CsrfTokenResponse;
import com.example.turniring.user.dto.LoginRequest;
import com.example.turniring.user.dto.RegistrationUserRequest;
import com.example.turniring.user.entity.UserEntity;
import com.example.turniring.user.service.JwtService;
import com.example.turniring.user.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/registration")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthResponse> registrationUser(
            @Valid @RequestBody RegistrationUserRequest request,
            HttpServletResponse response
    ) {
        UserEntity user = userService.register(request);
        String token = userService.issueToken(user);
        jwtService.attachAuthCookie(token, response);
        return new ResponseEntity<>(AuthResponse.from(user, token), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<AuthResponse> loginUser(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        UserEntity user = userService.login(request);
        String token = userService.issueToken(user);
        jwtService.attachAuthCookie(token, response);
        return ResponseEntity.ok(AuthResponse.from(user, token));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout current user")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        jwtService.clearAuthCookie(response);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<AuthResponse> me(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserEntity user = userService.getByEmail(authentication.getName());
        return ResponseEntity.ok(AuthResponse.from(user, null));
    }

    @GetMapping("/csrf")
    @Operation(summary = "Get CSRF token")
    public ResponseEntity<CsrfTokenResponse> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok(new CsrfTokenResponse(
                csrfToken.getToken(),
                csrfToken.getHeaderName(),
                csrfToken.getParameterName()
        ));
    }
}
