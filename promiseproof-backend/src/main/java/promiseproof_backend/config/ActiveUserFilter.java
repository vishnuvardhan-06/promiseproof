package promiseproof_backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import promiseproof_backend.entity.User;
import promiseproof_backend.repository.UserRepository;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Keeps an existing session in step with the users table.
 *
 * <p>A disabled or deleted account loses access on its next request, and a role
 * change is picked up immediately rather than waiting for the user to sign in
 * again. The session still stores the login; only the authorities are refreshed.
 */
public class ActiveUserFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public ActiveUserFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken ||
                "anonymousUser".equals(authentication.getName())) {

            filterChain.doFilter(request, response);
            return;
        }

        Optional<User> found =
                userRepository.findByEmailIgnoreCase(authentication.getName());

        if (found.isEmpty() || !found.get().isEnabled()) {

            SecurityContextHolder.clearContext();

            HttpSession session = request.getSession(false);

            if (session != null) {
                session.invalidate();
            }

            filterChain.doFilter(request, response);
            return;
        }

        User user = found.get();
        String expectedAuthority = "ROLE_" + user.getRole().name();

        boolean current = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        expectedAuthority.equals(authority.getAuthority()));

        if (!current) {

            UsernamePasswordAuthenticationToken refreshed =
                    UsernamePasswordAuthenticationToken.authenticated(
                            user.getEmail(),
                            authentication.getCredentials(),
                            List.of(new SimpleGrantedAuthority(expectedAuthority))
                    );

            refreshed.setDetails(authentication.getDetails());

            SecurityContextHolder.getContext().setAuthentication(refreshed);
        }

        filterChain.doFilter(request, response);
    }
}
