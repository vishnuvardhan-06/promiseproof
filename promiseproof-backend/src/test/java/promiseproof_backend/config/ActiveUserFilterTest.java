package promiseproof_backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import promiseproof_backend.entity.Role;
import promiseproof_backend.entity.User;
import promiseproof_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ActiveUserFilterTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ActiveUserFilter activeUserFilter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void appliesARoleChangeWithoutANewLogin() throws Exception {

        signInAs("reviewer@example.com", "ROLE_CITIZEN");

        when(userRepository.findByEmailIgnoreCase("reviewer@example.com"))
                .thenReturn(Optional.of(user("reviewer@example.com", Role.REVIEWER, true)));

        filter();

        assertEquals(
                "ROLE_REVIEWER",
                SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getAuthorities()
                        .iterator()
                        .next()
                        .getAuthority()
        );
    }

    @Test
    void clearsTheContextForADisabledAccount() throws Exception {

        signInAs("disabled@example.com", "ROLE_CITIZEN");

        when(userRepository.findByEmailIgnoreCase("disabled@example.com"))
                .thenReturn(Optional.of(user("disabled@example.com", Role.CITIZEN, false)));

        filter();

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void leavesAnUnchangedRoleAlone() throws Exception {

        signInAs("citizen@example.com", "ROLE_CITIZEN");

        when(userRepository.findByEmailIgnoreCase("citizen@example.com"))
                .thenReturn(Optional.of(user("citizen@example.com", Role.CITIZEN, true)));

        filter();

        assertEquals(
                "ROLE_CITIZEN",
                SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getAuthorities()
                        .iterator()
                        .next()
                        .getAuthority()
        );
    }

    private void filter() throws Exception {
        activeUserFilter.doFilter(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                new MockFilterChain()
        );
    }

    private static void signInAs(String email, String authority) {

        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        email,
                        null,
                        List.of(new SimpleGrantedAuthority(authority))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static User user(String email, Role role, boolean enabled) {

        User user = new User();

        user.setName("Test User");
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setRole(role);
        user.setEnabled(enabled);

        return user;
    }
}
