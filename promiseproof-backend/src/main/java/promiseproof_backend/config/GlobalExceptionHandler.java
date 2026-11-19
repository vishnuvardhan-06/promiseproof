package promiseproof_backend.config;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import promiseproof_backend.dto.ApiError;

import java.time.LocalDateTime;

/**
 * Converts exceptions into a small, consistent JSON error body.
 * Stack traces are never sent to clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(
            ResponseStatusException exception,
            HttpServletRequest request) {

        HttpStatusCode statusCode = exception.getStatusCode();

        String message = exception.getReason() != null
                ? exception.getReason()
                : "Request failed";

        return build(statusCode, message, request);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiError> handleMissingPart(
            MissingServletRequestPartException exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.BAD_REQUEST,
                "Required form field is missing: " +
                        exception.getRequestPartName(),
                request
        );
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<ApiError> handleBinding(
            ServletRequestBindingException exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.BAD_REQUEST,
                "Invalid or missing request parameter",
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Invalid request");

        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.BAD_REQUEST,
                "Request body is missing or malformed",
                request
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            NoResourceFoundException exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                request
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleUploadTooLarge(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Uploaded file is too large",
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.CONFLICT,
                "This record is still referenced by other data",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {

        log.error("Unhandled API error", exception);

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred",
                request
        );
    }

    private ResponseEntity<ApiError> build(
            HttpStatusCode statusCode,
            String message,
            HttpServletRequest request) {

        HttpStatus resolved = HttpStatus.resolve(statusCode.value());

        String reason = resolved != null
                ? resolved.getReasonPhrase()
                : "Error";

        ApiError body = new ApiError(
                LocalDateTime.now(),
                statusCode.value(),
                reason,
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(statusCode).body(body);
    }
}
