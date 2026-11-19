package promiseproof_backend.config;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;

import promiseproof_backend.repository.UserRepository;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            UserRepository userRepository) throws Exception {

        ActiveUserFilter activeUserFilter =
                new ActiveUserFilter(userRepository);

        // CSRF token is issued as a readable cookie so the SPA can echo it in
        // the X-XSRF-TOKEN header on every state-changing request.
        CookieCsrfTokenRepository tokenRepository =
                CookieCsrfTokenRepository.withHttpOnlyFalse();

        tokenRepository.setCookiePath("/");
        tokenRepository.setCookieName("XSRF-TOKEN");
        tokenRepository.setHeaderName("X-XSRF-TOKEN");

        XorCsrfTokenRequestAttributeHandler csrfHandler =
                new XorCsrfTokenRequestAttributeHandler();

        // Resolve the token from the request header rather than the masked
        // request attribute (the standard SPA configuration).
        csrfHandler.setCsrfRequestAttributeName(null);

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(tokenRepository)
                        .csrfTokenRequestHandler(csrfHandler))
                .cors(Customizer.withDefaults())
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository))
                .addFilterAfter(
                        activeUserFilter,
                        org.springframework.security.web.context
                                .SecurityContextHolderFilter.class)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public authentication endpoints.
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/logout",
                                "/api/auth/csrf"
                        ).permitAll()



                        // Personal collections need a signed-in user and are
                        // declared before the public read-only rules below.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/promises/mine"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/promises/search"
                        ).permitAll()

                        // Publication review queue: reviewers and admins. Must
                        // be declared before the public GET rules below.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/promises/publication-queue"
                        ).hasAnyRole("REVIEWER", "ADMIN")

                        // Truly public, read-only citizen views. Declared
                        // before the generic authenticated rules below. Each
                        // service enforces publication visibility and strips
                        // private submitter/reviewer information.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/promises/public",
                                "/api/promises/*",
                                "/api/promises/*/history",
                                "/api/evidence/promise/*/approved",
                                "/api/stats/public"
                        ).permitAll()

                        // Publication decision: reviewers and admins.
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/promises/*/publication"
                        ).hasAnyRole("REVIEWER", "ADMIN")

                        // Submitting a draft and managing its original sources
                        // stays with researchers and administrators; ownership
                        // is enforced in the service layer.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/promises/*/submit-for-publication"
                        ).hasAnyRole("RESEARCHER", "ADMIN")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/promises/*/sources"
                        ).hasAnyRole("RESEARCHER", "ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/promises/*/sources/*"
                        ).hasAnyRole("RESEARCHER", "ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/promises/*/sources/*"
                        ).hasAnyRole("RESEARCHER", "ADMIN")

                        // Public read-only views.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/parties/**",
                                "/api/elections/**",
                                "/api/promises/**",
                                "/api/stats/**"
                        ).authenticated()

                        // Public evidence: approved records only.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/evidence/promise/*/approved"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/evidence/*/download"
                        ).authenticated()

                        // A signed-in user can always read their own uploads.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/evidence/mine"
                        ).authenticated()

                        // Evidence queues are reviewer-only.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/evidence/pending"
                        ).hasAnyRole("REVIEWER", "ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/evidence/promise/*"
                        ).hasAnyRole("REVIEWER", "ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/evidence/**"
                        ).hasAnyRole("REVIEWER", "ADMIN")

                        // Any signed-in user may submit evidence.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/evidence"
                        ).authenticated()

                        // Controlled resubmission: the submitter or an
                        // administrator (checked in the service).
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/evidence/*/resubmit"
                        ).authenticated()

                        // Only reviewers and admins may verify evidence.
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/evidence/*/review"
                        ).hasAnyRole("REVIEWER", "ADMIN")

                        // Party and election records are admin-managed.
                        .requestMatchers(
                                "/api/parties/**",
                                "/api/elections/**"
                        ).hasRole("ADMIN")

                        // Promise status verification: researchers propose
                        // changes backed by approved evidence; reviewers and
                        // administrators decide.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/status-proposals/pending"
                        ).hasAnyRole("REVIEWER", "ADMIN")
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/status-proposals/*/decision"
                        ).hasAnyRole("REVIEWER", "ADMIN")
                        .requestMatchers("/api/status-proposals/**")
                        .hasAnyRole("RESEARCHER", "ADMIN")

                        // Administrative status corrections are a separate,
                        // audited exception path and are restricted to admins.
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/promises/*/status"
                        ).hasRole("ADMIN")

                        // Researchers and admins manage promises.
                        .requestMatchers("/api/promises/**")
                        .hasAnyRole("ADMIN", "RESEARCHER")

                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        // Platform-wide audit trail is administrator-only.
                        .requestMatchers("/api/audit/**").hasRole("ADMIN")

                        .requestMatchers("/api/notifications/**")
                        .authenticated()

                        .anyRequest().authenticated()
                )
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, error) ->
                                writeError(
                                        response,
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "Unauthorized",
                                        "Please sign in to continue"
                                ))
                        .accessDeniedHandler((request, response, error) ->
                                writeError(
                                        response,
                                        HttpServletResponse.SC_FORBIDDEN,
                                        "Forbidden",
                                        "You do not have permission to perform this action"
                                ))
                );

        return http.build();
    }

    private static void writeError(
            HttpServletResponse response,
            int status,
            String error,
            String message) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");

        response.getWriter().write(
                "{\"status\":" + status +
                        ",\"error\":\"" + error +
                        "\",\"message\":\"" + message + "\"}"
        );
    }
}
