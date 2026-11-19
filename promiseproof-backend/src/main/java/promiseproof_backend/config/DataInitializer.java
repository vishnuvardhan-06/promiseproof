package promiseproof_backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import promiseproof_backend.entity.Role;
import promiseproof_backend.entity.User;
import promiseproof_backend.repository.UserRepository;

import java.util.regex.Pattern;

/**
 * Creates the first administrator account on startup when none exists.
 *
 * The administrator password must be supplied through the environment (for
 * example PROMISEPROOF_ADMIN_PASSWORD) and must meet a minimum strength
 * requirement. No predictable default password is ever accepted, so a fresh
 * deployment cannot boot with published credentials.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(DataInitializer.class);

    private static final Pattern UPPERCASE =
            Pattern.compile("[A-Z]");

    private static final Pattern LOWERCASE =
            Pattern.compile("[a-z]");

    private static final Pattern DIGIT =
            Pattern.compile("[0-9]");

    private static final int MIN_PASSWORD_LENGTH = 10;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${promiseproof.admin.name:Platform Administrator}")
    private String adminName;

    @Value("${promiseproof.admin.email:admin@promiseproof.local}")
    private String adminEmail;

    @Value("${promiseproof.admin.password:}")
    private String adminPassword;

    public DataInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {

        if (userRepository.existsByEmailIgnoreCase(adminEmail)) {
            return;
        }

        String password = adminPassword == null
                ? ""
                : adminPassword.trim();

        if (password.isBlank()) {
            throw new IllegalStateException(
                    "No administrator password configured. Set the " +
                            "PROMISEPROOF_ADMIN_PASSWORD environment variable " +
                            "to a strong initial administrator password before " +
                            "starting the backend."
            );
        }

        if (password.length() < MIN_PASSWORD_LENGTH ||
                !UPPERCASE.matcher(password).find() ||
                !LOWERCASE.matcher(password).find() ||
                !DIGIT.matcher(password).find()) {

            throw new IllegalStateException(
                    "The configured administrator password is too weak. " +
                            "Use a password of at least " + MIN_PASSWORD_LENGTH +
                            " characters that includes uppercase, lowercase " +
                            "and numeric characters."
            );
        }

        User admin = new User();
        admin.setName(adminName);
        admin.setEmail(adminEmail.toLowerCase());
        admin.setPasswordHash(
                passwordEncoder.encode(password)
        );
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);

        userRepository.save(admin);

        log.info(
                "Created initial administrator account: {}",
                adminEmail
        );
    }
}
