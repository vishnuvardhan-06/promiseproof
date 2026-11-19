package promiseproof_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import promiseproof_backend.dto.UpdateUserRequest;
import promiseproof_backend.dto.UserResponse;
import promiseproof_backend.entity.Role;
import promiseproof_backend.entity.User;
import promiseproof_backend.repository.UserRepository;
import promiseproof_backend.service.AuditService;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private UserService userService;

    @Test
    void cannotDemoteTheLastActiveAdministrator() {

        User admin = user("admin@promiseproof.local", Role.ADMIN);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndEnabledTrue(Role.ADMIN))
                .thenReturn(1L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.updateUser(
                        1L,
                        new UpdateUserRequest("RESEARCHER", null),
                        "second-admin@promiseproof.local"
                )
        );

        assertEquals(409, exception.getStatusCode().value());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void cannotChangeOwnRole() {

        User admin = user("admin@promiseproof.local", Role.ADMIN);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.updateUser(
                        1L,
                        new UpdateUserRequest("CITIZEN", null),
                        "admin@promiseproof.local"
                )
        );

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void promotesCitizenWhenAnotherAdminExists() {

        User citizen = user("citizen@example.com", Role.CITIZEN);

        when(userRepository.findById(2L)).thenReturn(Optional.of(citizen));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateUser(
                2L,
                new UpdateUserRequest("REVIEWER", null),
                "admin@promiseproof.local"
        );

        assertEquals("REVIEWER", response.role());

        verify(notificationService).notifyUser(
                eq("citizen@example.com"),
                eq("ACCOUNT_ROLE_CHANGED"),
                anyString(),
                anyString(),
                anyString()
        );

        // Guard query is only needed when an admin is being demoted.
        verify(userRepository, never())
                .countByRoleAndEnabledTrue(Role.ADMIN);
    }

    @Test
    void rejectsUnknownRole() {
        User citizen = user("citizen@example.com", Role.CITIZEN);

        when(userRepository.findById(2L)).thenReturn(Optional.of(citizen));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.updateUser(
                        2L,
                        new UpdateUserRequest("SUPERUSER", null),
                        "admin@promiseproof.local"
                )
        );

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void roleChangeAuditAttributesTheActingAdministrator() {

        User citizen = user("citizen@example.com", Role.CITIZEN);
        User admin = user("admin@example.com", Role.ADMIN);

        when(userRepository.findById(2L)).thenReturn(Optional.of(citizen));
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateUser(
                2L,
                new UpdateUserRequest("RESEARCHER", null),
                "admin@example.com"
        );

        verify(auditService).record(
                eq("USER_ROLE_CHANGED"),
                eq("USER"),
                nullable(Long.class),
                eq("Test User"),
                eq("admin@example.com"),
                eq("ADMIN"),
                contains("CITIZEN -> RESEARCHER")
        );
    }

    @Test
    void disablingAnAccountRecordsUserDisabledWithActor() {

        User citizen = user("citizen@example.com", Role.CITIZEN);
        User admin = user("admin@example.com", Role.ADMIN);

        when(userRepository.findById(2L)).thenReturn(Optional.of(citizen));
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateUser(
                2L,
                new UpdateUserRequest(null, false),
                "admin@example.com"
        );

        verify(auditService).record(
                eq("USER_DISABLED"),
                eq("USER"),
                nullable(Long.class),
                eq("Test User"),
                eq("admin@example.com"),
                eq("ADMIN"),
                contains("Enabled: true -> false")
        );
    }

    private static User user(String email, Role role) {

        User user = new User();

        user.setName("Test User");
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setRole(role);
        user.setEnabled(true);

        return user;
    }
}
