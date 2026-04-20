package com.example.turniring.user.controller;

import com.example.turniring.user.dto.LoginRequest;
import com.example.turniring.user.dto.RegistrationUserRequest;
import com.example.turniring.user.entity.UserEntity;
import com.example.turniring.user.entity.UserRole;
import com.example.turniring.user.service.JwtService;
import com.example.turniring.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private JwtService jwtService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(userService, jwtService);
    }

    @Test
    void registrationReturnsCreatedAndAttachesCookie() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .name("Alice")
                .lastName("Johnson")
                .email("alice@example.com")
                .role(UserRole.TEAM)
                .build();
        RegistrationUserRequest request = new RegistrationUserRequest(
                "Alice",
                "Johnson",
                "alice@example.com",
                "password123",
                UserRole.USER
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(userService.register(request)).thenReturn(user);
        when(userService.issueToken(user)).thenReturn("jwt-token");

        var result = authController.registrationUser(request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().token()).isEqualTo("jwt-token");
        verify(jwtService).attachAuthCookie("jwt-token", response);
    }

    @Test
    void loginReturnsOkAndTokenPayload() {
        UserEntity user = UserEntity.builder()
                .id(2L)
                .name("Bob")
                .lastName("Smith")
                .email("bob@example.com")
                .role(UserRole.JURY)
                .build();
        LoginRequest request = new LoginRequest("bob@example.com", "password123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(userService.login(request)).thenReturn(user);
        when(userService.issueToken(user)).thenReturn("jury-token");

        var result = authController.loginUser(request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().role()).isEqualTo("JURY");
        assertThat(result.getBody().token()).isEqualTo("jury-token");
        verify(jwtService).attachAuthCookie("jury-token", response);
    }

    @Test
    void meReturnsUnauthorizedWhenAuthenticationMissing() {
        var result = authController.me(null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(userService);
    }
}
