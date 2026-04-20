package com.example.turniring.user.service;

import com.example.turniring.user.dto.CreateUserRequest;
import com.example.turniring.user.dto.LoginRequest;
import com.example.turniring.user.dto.RegistrationUserRequest;
import com.example.turniring.user.entity.UserEntity;
import com.example.turniring.user.entity.UserRole;
import com.example.turniring.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.bootstrap.admin.enabled:true}")
    private boolean bootstrapAdminEnabled;

    @Value("${app.bootstrap.admin.name:System}")
    private String bootstrapAdminName;

    @Value("${app.bootstrap.admin.last-name:Admin}")
    private String bootstrapAdminLastName;

    @Value("${app.bootstrap.admin.email:admin@turniring.local}")
    private String bootstrapAdminEmail;

    @Value("${app.bootstrap.admin.password}")
    private String bootstrapAdminPassword;

    @Transactional
    public UserEntity register(@NonNull RegistrationUserRequest data) {
        UserRole registrationRole = resolveSelfAssignableRole(data.role(), UserRole.TEAM);
        return createUser(
                data.name(),
                data.lastName(),
                data.email(),
                data.password(),
                registrationRole
        );
    }

    @Transactional
    public UserEntity login(@NonNull LoginRequest data) {
        UserEntity user = userRepository.findByEmail(normalizeEmail(data.email()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"
                ));

        if (!passwordEncoder.matches(data.password(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        return ensureRoleAssigned(user);
    }

    @Transactional
    public UserEntity getByEmail(@NonNull String email) {
        UserEntity user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"
                ));
        return ensureRoleAssigned(user);
    }

    public String issueToken(@NonNull UserEntity user) {
        return jwtService.generateToken(user);
    }

    @Transactional
    public UserEntity createUser(@NonNull CreateUserRequest request) {
        if (request.role() == UserRole.JURY) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "JURY role can be granted only through invite links"
            );
        }
        return createUser(
                request.name(),
                request.lastName(),
                request.email(),
                request.password(),
                request.role()
        );
    }

    @Transactional(readOnly = true)
    public List<UserEntity> getByRole(@NonNull UserRole role) {
        return userRepository.findAllByRole(role);
    }

    @Transactional
    public void updateProfileData(
            @NonNull UserEntity user,
            @NonNull String name,
            @NonNull String lastName,
            @NonNull String email,
            String newPassword
    ) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmailAndIdNot(normalizedEmail, user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User with email " + normalizedEmail + " already exists"
            );
        }

        user.setName(name.trim());
        user.setLastName(lastName.trim());
        user.setEmail(normalizedEmail);
        if (newPassword != null && !newPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(newPassword));
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void bootstrapAdminIfMissing() {
        if (!bootstrapAdminEnabled || userRepository.existsByEmail(normalizeEmail(bootstrapAdminEmail))) {
            return;
        }

        createUser(
                bootstrapAdminName,
                bootstrapAdminLastName,
                bootstrapAdminEmail,
                bootstrapAdminPassword,
                UserRole.ADMIN
        );
    }

    private UserEntity createUser(String name, String lastName, String email, String password, UserRole role) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User with email " + normalizedEmail + " already exists"
            );
        }

        UserEntity user = UserEntity.builder()
                .name(name)
                .lastName(lastName)
                .email(normalizedEmail)
                .password(passwordEncoder.encode(password))
                .role(role)
                .build();

        return userRepository.save(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserEntity ensureRoleAssigned(UserEntity user) {
        if (user.getRole() != null) {
            return user;
        }
        user.setRole(UserRole.USER);
        return userRepository.save(user);
    }

    public UserRole resolveSelfAssignableRole(UserRole requestedRole, UserRole defaultRole) {
        UserRole resolvedRole = requestedRole == null ? defaultRole : requestedRole;
        if (!resolvedRole.isSelfAssignable()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Role can be set only to TEAM, USER, or ADMIN"
            );
        }
        return resolvedRole;
    }
}
