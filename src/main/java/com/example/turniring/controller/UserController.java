package com.example.turniring.controller;

import com.example.turniring.dto.RegistrationUserRequest;
import com.example.turniring.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @PostMapping("/registration")
    public Long registrationUser(@Valid @RequestBody RegistrationUserRequest request) {
        return userService.saveUser(request);
    }
}