package promiseproof_backend.dto;

import java.time.LocalDateTime;

// Standard error payload returned by the API.
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
