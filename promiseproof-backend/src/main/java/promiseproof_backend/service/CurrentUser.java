package promiseproof_backend.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import promiseproof_backend.entity.User;
import promiseproof_backend.repository.UserRepository;

import java.util.Optional;

/**
 * Resolves the signed-in user for services that need to attribute actions.
 * Returns empty for anonymous requests.
 */
@Component
public class CurrentUser {

    private final UserRepository userRepository;

    public CurrentUser(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> get() {

        Authentication authentication = authentication();

        if (authentication == null) {
            return Optional.empty();
        }

        return userRepository.findByEmailIgnoreCase(authentication.getName());
    }

    public String email() {

        Optional<User> user = get();

        if (user.isPresent()) {
            return user.get().getEmail();
        }

        Authentication authentication = authentication();

        return authentication == null ? null : authentication.getName();
    }

    public String displayName() {
        return get().map(User::getName).orElse(null);
    }

    public String role() {
        return get()
                .map(user -> user.getRole() == null ? null : user.getRole().name())
                .orElse("SYSTEM");
    }

    private Authentication authentication() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getName())) {

            return null;
        }

        return authentication;
    }
}
