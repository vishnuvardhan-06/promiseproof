package promiseproof_backend.dto;

/**
 * Administrator edit of an account. Either field may be omitted; only the
 * supplied values are applied.
 */
public record UpdateUserRequest(
        String role,
        Boolean enabled
) {
}
