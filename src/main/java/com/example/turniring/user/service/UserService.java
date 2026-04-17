package com.example.turniring.service;

import com.example.turniring.dto.RegistrationUserRequest;
import com.example.turniring.entity.UserEntity;
import com.example.turniring.entity.UserRole;
import com.example.turniring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Long saveUser(@NonNull RegistrationUserRequest data) {
        if (userRepository.existsByEmail(data.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The user's email address already exists."
            );
        }

        var user = new UserEntity();

        user.setName(data.name());
        user.setLastName(data.lastName());
        user.setEmail(data.email());
        user.setPassword(passwordEncoder.encode(data.password()));
        user.setRole(UserRole.USER);

        var savedUser = userRepository.save(user);

        return savedUser.getId();
    }
}
