package promiseproof_backend.config;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import promiseproof_backend.entity.User;
import promiseproof_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    void createsAdminWhenNoneExists() {

        ReflectionTestUtils.setField(
                dataInitializer,
                "adminEmail",
                "admin@example.com"
        );
        ReflectionTestUtils.setField(
                dataInitializer,
                "adminName",
                "Platform Administrator"
        );
        ReflectionTestUtils.setField(
                dataInitializer,
                "adminPassword",
                "SuperSecret123"
        );

        when(userRepository.existsByEmailIgnoreCase("admin@example.com"))
                .thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        dataInitializer.run(null);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void failsStartupWhenTheAdminPasswordIsMissing() {

        ReflectionTestUtils.setField(
                dataInitializer,
                "adminEmail",
                "admin@example.com"
        );
        ReflectionTestUtils.setField(
                dataInitializer,
                "adminPassword",
                "   "
        );

        when(userRepository.existsByEmailIgnoreCase("admin@example.com"))
                .thenReturn(false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> dataInitializer.run(null)
        );

        assertTrue(exception.getMessage().contains("PROMISEPROOF_ADMIN_PASSWORD"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void failsStartupWhenTheAdminPasswordIsWeak() {

        ReflectionTestUtils.setField(
                dataInitializer,
                "adminEmail",
                "admin@example.com"
        );
        ReflectionTestUtils.setField(
                dataInitializer,
                "adminPassword",
                "weakpassword"
        );

        when(userRepository.existsByEmailIgnoreCase("admin@example.com"))
                .thenReturn(false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> dataInitializer.run(null)
        );

        assertTrue(exception.getMessage().contains("too weak"));
        verify(userRepository, never()).save(any(User.class));
    }
}
