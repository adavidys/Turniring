package com.example.turniring.user.service;

import com.example.turniring.support.TestFixtures;
import com.example.turniring.user.dto.CreateUserRequest;
import com.example.turniring.user.dto.LoginRequest;
import com.example.turniring.user.dto.RegistrationUserRequest;
import com.example.turniring.user.entity.UserEntity;
import com.example.turniring.user.entity.UserRole;
import com.example.turniring.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerNormalizesEmailAndPersistsSelectedRole() {
        RegistrationUserRequest request = new RegistrationUserRequest(
                "Alice",
                "Johnson",
                "  Alice@Example.COM ",
                "password123",
                UserRole.TEAM
        );

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });

        UserEntity created = userService.register(request);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        UserEntity savedUser = captor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo("alice@example.com");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.TEAM);
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(created.getId()).isEqualTo(42L);
    }

    @Test
    void registerRejectsOrganizerRole() {
        RegistrationUserRequest request = new RegistrationUserRequest(
                "Alice",
                "Johnson",
                "alice@example.com",
                "password123",
                UserRole.ORGANIZER
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.register(request)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo("Role can be set only to TEAM, USER, or ADMIN");
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void loginRejectsInvalidPassword() {
        UserEntity user = TestFixtures.user(7L, "alice@example.com", UserRole.TEAM);
        user.setPassword("encoded-password");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "encoded-password")).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.login(new LoginRequest(" Alice@Example.com ", "wrongpass"))
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exception.getReason()).isEqualTo("Invalid email or password");
    }

    @Test
    void loginAssignsDefaultRoleForLegacyUsersWithoutRole() {
        UserEntity user = TestFixtures.user(8L, "legacy@example.com", UserRole.TEAM);
        user.setRole(null);
        user.setPassword("encoded-password");

        when(userRepository.findByEmail("legacy@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserEntity loggedIn = userService.login(new LoginRequest("legacy@example.com", "password123"));

        assertThat(loggedIn.getRole()).isEqualTo(UserRole.USER);
        verify(userRepository).save(user);
    }

    @Test
    void getByEmailAssignsDefaultRoleForLegacyUsersWithoutRole() {
        UserEntity user = TestFixtures.user(9L, "legacy-get@example.com", UserRole.TEAM);
        user.setRole(null);

        when(userRepository.findByEmail("legacy-get@example.com")).thenReturn(java.util.Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserEntity fetched = userService.getByEmail("legacy-get@example.com");

        assertThat(fetched.getRole()).isEqualTo(UserRole.USER);
        verify(userRepository).save(user);
    }

    @Test
    void bootstrapAdminIfMissingCreatesConfiguredAdmin() {
        ReflectionTestUtils.setField(userService, "bootstrapAdminEnabled", true);
        ReflectionTestUtils.setField(userService, "bootstrapAdminName", "System");
        ReflectionTestUtils.setField(userService, "bootstrapAdminLastName", "Admin");
        ReflectionTestUtils.setField(userService, "bootstrapAdminEmail", " Admin@Turniring.local ");
        ReflectionTestUtils.setField(userService, "bootstrapAdminPassword", "Admin12345");

        when(userRepository.existsByEmail("admin@turniring.local")).thenReturn(false);
        when(passwordEncoder.encode("Admin12345")).thenReturn("encoded-admin");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.bootstrapAdminIfMissing();

        verify(userRepository, times(2)).existsByEmail("admin@turniring.local");
        verify(userRepository).save(argThat(user ->
                user.getRole() == UserRole.ADMIN
                        && user.getEmail().equals("admin@turniring.local")
                        && user.getPassword().equals("encoded-admin")
        ));
    }

    @Test
    void createUserRejectsDuplicateEmail() {
        CreateUserRequest request = new CreateUserRequest(
                "John",
                "Smith",
                "john@example.com",
                "password123",
                UserRole.TEAM
        );
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> userService.createUser(request));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createUserRejectsJuryRole() {
        CreateUserRequest request = new CreateUserRequest(
                "John",
                "Smith",
                "john@example.com",
                "password123",
                UserRole.JURY
        );

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> userService.createUser(request));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo("JURY role can be granted only through invite links");
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void updateProfileDataUpdatesFieldsAndPassword() {
        UserEntity user = TestFixtures.user(77L, "old@example.com", UserRole.ADMIN);
        user.setName("Old");
        user.setLastName("Name");
        user.setPassword("old-hash");

        when(userRepository.existsByEmailAndIdNot("new@example.com", 77L)).thenReturn(false);
        when(passwordEncoder.encode("newpassword123")).thenReturn("new-hash");

        userService.updateProfileData(
                user,
                "New",
                "Person",
                " New@Example.com ",
                "newpassword123"
        );

        assertThat(user.getName()).isEqualTo("New");
        assertThat(user.getLastName()).isEqualTo("Person");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getPassword()).isEqualTo("new-hash");
    }

    @Test
    void updateProfileDataRejectsDuplicateEmail() {
        UserEntity user = TestFixtures.user(78L, "old@example.com", UserRole.JURY);
        when(userRepository.existsByEmailAndIdNot("taken@example.com", 78L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.updateProfileData(user, "Name", "Last", "taken@example.com", null)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(passwordEncoder, never()).encode(anyString());
    }
}
