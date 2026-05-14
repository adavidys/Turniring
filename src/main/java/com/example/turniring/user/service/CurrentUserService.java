package com.example.turniring.user.service;

import com.example.turniring.user.entity.UserEntity;
import com.example.turniring.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserService userService;

    public UserEntity requireCurrentUser() {
        return getCurrentUser().orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required")
        );
    }

    public Optional<UserEntity> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            return Optional.empty();
        }

        return Optional.of(userService.getByEmail(authentication.getName()));
    }

    public void requireRole(UserRole... roles) {
        UserEntity user = requireCurrentUser();
        boolean allowed = Arrays.stream(roles).anyMatch(role -> role == user.getRole());
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
        }
    }
}
