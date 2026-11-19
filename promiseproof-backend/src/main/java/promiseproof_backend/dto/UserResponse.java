package promiseproof_backend.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        String role,
        boolean enabled,
        LocalDateTime createdAt
) {
}
