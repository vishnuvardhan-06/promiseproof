package promiseproof_backend.service;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import promiseproof_backend.dto.AuthResponse;
import promiseproof_backend.dto.RegisterRequest;
import promiseproof_backend.dto.UpdateUserRequest;
import promiseproof_backend.dto.UserResponse;
import promiseproof_backend.entity.Role;
import promiseproof_backend.entity.User;
import promiseproof_backend.repository.UserRepository;

import java.util.List;
import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            NotificationService notificationService,
            AuditService auditService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists"
            );
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );
        user.setRole(Role.CITIZEN);
        user.setEnabled(true);

        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {

        if (email == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Not signed in"
            );
        }

        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Account no longer exists"
                ));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {

        return userRepository
                .findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(this::toUserResponse)
                .toList();
    }

    /**
     * Change a user's role or enable/disable their account. Guards keep at
     * least one active administrator and stop an administrator from locking
     * themselves out.
     */
    @Transactional
    public UserResponse updateUser(
            Long id,
            UpdateUserRequest request,
            String actingEmail) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        Role currentRole = user.getRole();
        Role newRole = currentRole;
        boolean wasEnabled = user.isEnabled();

        if (request.role() != null && !request.role().isBlank()) {

            try {
                newRole = Role.valueOf(
                        request.role().trim().toUpperCase(Locale.ROOT)
                );
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unknown role. Allowed values: CITIZEN, " +
                                "RESEARCHER, REVIEWER, ADMIN"
                );
            }
        }

        Boolean enabled = request.enabled();

        boolean roleChanged = newRole != currentRole;
        boolean disabling = Boolean.FALSE.equals(enabled) && user.isEnabled();
        boolean actingOnSelf = actingEmail != null &&
                actingEmail.equalsIgnoreCase(user.getEmail());

        if (actingOnSelf && (roleChanged || disabling)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot change your own role or disable your own account"
            );
        }

        boolean removesActiveAdmin = currentRole == Role.ADMIN &&
                user.isEnabled() &&
                ((roleChanged && newRole != Role.ADMIN) || disabling);

        if (removesActiveAdmin &&
                userRepository.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "At least one active administrator must remain"
            );
        }

        user.setRole(newRole);

        if (enabled != null) {
            user.setEnabled(enabled);
        }

        User saved = userRepository.save(user);

        boolean nowEnabled = saved.isEnabled();
        boolean enabledChanged = nowEnabled != wasEnabled;

        // The audit trail must attribute the action to the administrator who
        // performed it, not to the account being changed. The affected account
        // stays on the record as the target (entity id/title).
        User actor = actingEmail == null
                ? null
                : userRepository.findByEmailIgnoreCase(actingEmail)
                        .orElse(null);

        String actorEmail = actor != null
                ? actor.getEmail()
                : actingEmail;
        String actorRole = actor != null && actor.getRole() != null
                ? actor.getRole().name()
                : null;

        StringBuilder details = new StringBuilder();

        details.append("Target account: ").append(saved.getEmail());

        if (roleChanged) {
            details.append(". Role: ")
                    .append(currentRole == null ? "none" : currentRole.name())
                    .append(" -> ")
                    .append(newRole == null ? "none" : newRole.name());
        }

        if (enabledChanged) {
            details.append(". Enabled: ")
                    .append(wasEnabled)
                    .append(" -> ")
                    .append(nowEnabled);
        }

        if (roleChanged) {
            auditService.record(
                    "USER_ROLE_CHANGED",
                    "USER",
                    saved.getId(),
                    saved.getName(),
                    actorEmail,
                    actorRole,
                    details.toString()
            );
        }

        if (enabledChanged) {
            auditService.record(
                    nowEnabled ? "USER_ENABLED" : "USER_DISABLED",
                    "USER",
                    saved.getId(),
                    saved.getName(),
                    actorEmail,
                    actorRole,
                    details.toString()
            );
        }

        if (!roleChanged && !enabledChanged) {
            auditService.record(
                    "USER_UPDATED",
                    "USER",
                    saved.getId(),
                    saved.getName(),
                    actorEmail,
                    actorRole,
                    details.toString()
            );
        }

        if (roleChanged) {
            notificationService.notifyUser(
                    saved.getEmail(),
                    "ACCOUNT_ROLE_CHANGED",
                    "Your account role changed",
                    "Your PromiseProof role is now " + newRole.name() + ".",
                    "/dashboard"
            );
        }

        return toUserResponse(saved);
    }

    public AuthResponse toResponse(User user) {

        return new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    private UserResponse toUserResponse(User user) {

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
