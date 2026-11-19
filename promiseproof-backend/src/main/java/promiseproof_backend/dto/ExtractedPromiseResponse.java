package promiseproof_backend.dto;

/**
 * A promise proposed by the manifesto assistant. Nothing is saved until a
 * researcher or administrator confirms the selection in the UI.
 */
public record ExtractedPromiseResponse(
        String title,
        String description
) {
}
