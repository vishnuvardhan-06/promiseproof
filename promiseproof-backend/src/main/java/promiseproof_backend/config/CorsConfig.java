package promiseproof_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Single source of CORS rules for both Spring MVC and Spring Security.
 * Allowed origins come from promiseproof.cors.allowed-origins so that a
 * deployment can list its real front-end URLs.
 */
@Configuration
public class CorsConfig {

    private final String allowedOrigins;

    public CorsConfig(
            @Value("${promiseproof.cors.allowed-origins:http://localhost:5173}")
            String allowedOrigins) {

        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        configuration.setAllowedOrigins(origins);

        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );

        // Includes X-XSRF-TOKEN, which the SPA sends on writes.
        configuration.setAllowedHeaders(List.of("*"));

        // Required so the browser sends the session cookie.
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }
}
