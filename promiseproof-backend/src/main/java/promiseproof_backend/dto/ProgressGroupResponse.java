package promiseproof_backend.dto;

import java.util.Map;

public record ProgressGroupResponse(
        Long id,
        String name,
        long total,
        Map<String, Long> statusCounts
) {
}
