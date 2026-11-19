package promiseproof_backend.dto;

import java.time.LocalDateTime;

public record PromiseStatusHistoryResponse(

        Long id,
        String fromStatus,
        String toStatus,
        String changedByEmail,
        String changedByName,
        String reason,
        LocalDateTime createdAt
) {
}
