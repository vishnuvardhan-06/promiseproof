package promiseproof_backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import promiseproof_backend.dto.PromiseSourceRequest;
import promiseproof_backend.dto.PromiseSourceResponse;
import promiseproof_backend.entity.Promise;
import promiseproof_backend.entity.PromiseSource;
import promiseproof_backend.entity.Role;
import promiseproof_backend.repository.PromiseRepository;
import promiseproof_backend.repository.PromiseSourceRepository;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Original-source tracking. Every published promise must remain traceable to
 * the document and exact wording it came from.
 */
@Service
public class PromiseSourceService {

    private static final Set<String> SOURCE_TYPES = Set.of(
            "MANIFESTO",
            "PARTY_DOCUMENT",
            "GOVERNMENT_ANNOUNCEMENT",
            "IMPLEMENTATION_REPORT",
            "OTHER"
    );

    private final PromiseRepository promiseRepository;
    private final PromiseSourceRepository sourceRepository;
    private final PromiseService promiseService;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public PromiseSourceService(
            PromiseRepository promiseRepository,
            PromiseSourceRepository sourceRepository,
            PromiseService promiseService,
            CurrentUser currentUser,
            AuditService auditService) {

        this.promiseRepository = promiseRepository;
        this.sourceRepository = sourceRepository;
        this.promiseService = promiseService;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PromiseSourceResponse> getSources(Long promiseId) {

        Promise promise = findPromise(promiseId);

        if (!promise.isPubliclyVisible() && !canManage(promise)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Promise not found"
            );
        }

        return list(promiseId);
    }

    @Transactional
    public PromiseSourceResponse createSource(
            Long promiseId,
            PromiseSourceRequest request) {

        Promise promise = findPromise(promiseId);

        assertCanManage(promise);

        validate(request);

        PromiseSource source = new PromiseSource();

        source.setPromise(promise);
        apply(source, request);
        source.setCreatedByEmail(currentUser.email());
        source.setCreatedByName(currentUser.displayName());

        PromiseSource saved = sourceRepository.save(source);

        reopenForReview(promise, "source added");

        auditService.record(
                "PROMISE_SOURCE_CREATED",
                "PROMISE",
                promise.getId(),
                promise.getTitle(),
                currentUser.email(),
                currentUser.role(),
                "Source: " + saved.getManifestoName() +
                        (saved.getPageNumber() == null
                                ? ""
                                : ", page " + saved.getPageNumber())
        );

        return toResponse(saved);
    }

    @Transactional
    public PromiseSourceResponse updateSource(
            Long promiseId,
            Long sourceId,
            PromiseSourceRequest request) {

        Promise promise = findPromise(promiseId);

        assertCanManage(promise);

        PromiseSource source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Source not found"
                ));

        if (!source.getPromise().getId().equals(promiseId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Source not found"
            );
        }

        validate(request);

        apply(source, request);

        PromiseSource saved = sourceRepository.save(source);

        reopenForReview(promise, "source updated");

        auditService.record(
                "PROMISE_SOURCE_UPDATED",
                "PROMISE",
                promise.getId(),
                promise.getTitle(),
                currentUser.email(),
                currentUser.role(),
                "Source: " + saved.getManifestoName()
        );

        return toResponse(saved);
    }

    @Transactional
    public void deleteSource(Long promiseId, Long sourceId) {

        Promise promise = findPromise(promiseId);

        assertCanManage(promise);

        PromiseSource source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Source not found"
                ));

        if (!source.getPromise().getId().equals(promiseId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Source not found"
            );
        }

        sourceRepository.delete(source);

        reopenForReview(promise, "source removed");

        auditService.record(
                "PROMISE_SOURCE_DELETED",
                "PROMISE",
                promise.getId(),
                promise.getTitle(),
                currentUser.email(),
                currentUser.role(),
                "Source: " + source.getManifestoName()
        );
    }

    private void reopenForReview(Promise promise, String reason) {

        if (Promise.PUBLISHED.equals(promise.getPublicationStatus()) ||
                Promise.SUBMITTED.equals(promise.getPublicationStatus()) ||
                Promise.REVIEWED.equals(promise.getPublicationStatus())) {

            promise.setPublicationStatus(Promise.DRAFT);
            promise.setPublicationComment(
                    "Returned to draft after " + reason
            );
            promise.setSubmittedAt(null);
            promise.setReviewedAt(null);
            promise.setPublishedAt(null);
            promiseRepository.save(promise);

            auditService.record(
                    "PROMISE_REOPENED_FOR_REVIEW",
                    "PROMISE",
                    promise.getId(),
                    promise.getTitle(),
                    currentUser.email(),
                    currentUser.role(),
                    reason
            );
        }
    }

    private Promise findPromise(Long promiseId) {

        return promiseRepository.findById(promiseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Promise not found"
                ));
    }

    private boolean canManage(Promise promise) {

        String role = currentUser.role();
        String email = currentUser.email();

        return Role.ADMIN.name().equals(role) ||
                (email != null &&
                        promise.getCreatedByEmail() != null &&
                        email.equalsIgnoreCase(promise.getCreatedByEmail()));
    }

    private void assertCanManage(Promise promise) {

        if (!canManage(promise)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the researcher who recorded this promise or an " +
                            "administrator can manage its sources"
            );
        }
    }

    private void validate(PromiseSourceRequest request) {

        if (request == null ||
                request.getManifestoName() == null ||
                request.getManifestoName().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Original manifesto or document name is required"
            );
        }

        if (request.getExactQuotation() == null ||
                request.getExactQuotation().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The exact original quotation is required"
            );
        }

        if (request.getPageNumber() != null &&
                request.getPageNumber() < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page number must be positive"
            );
        }

        validateUrl(request.getSourceUrl());

        if (request.getSourceType() != null &&
                !request.getSourceType().isBlank() &&
                !SOURCE_TYPES.contains(
                        request.getSourceType()
                                .trim()
                                .toUpperCase(Locale.ROOT))) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid source type. Allowed values: " +
                            String.join(", ", SOURCE_TYPES)
            );
        }
    }

    private void apply(PromiseSource source, PromiseSourceRequest request) {

        source.setManifestoName(request.getManifestoName().trim());
        source.setSourceUrl(trimToNull(request.getSourceUrl()));
        source.setPageNumber(request.getPageNumber());
        source.setExactQuotation(request.getExactQuotation().trim());
        source.setPublishedDate(request.getPublishedDate());

        source.setSourceType(
                request.getSourceType() == null ||
                        request.getSourceType().isBlank()
                        ? "MANIFESTO"
                        : request.getSourceType()
                                .trim()
                                .toUpperCase(Locale.ROOT)
        );
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static void validateUrl(String sourceUrl) {

        if (sourceUrl == null || sourceUrl.isBlank()) {
            return;
        }

        try {
            URI uri = new URI(sourceUrl.trim());
            String scheme = uri.getScheme();

            if (scheme == null ||
                    !(scheme.equalsIgnoreCase("http") ||
                            scheme.equalsIgnoreCase("https")) ||
                    uri.getHost() == null) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Source URL must be a valid http or https address"
                );
            }

        } catch (URISyntaxException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Source URL must be a valid http or https address"
            );
        }
    }

    public List<PromiseSourceResponse> list(Long promiseId) {

        return sourceRepository
                .findByPromise_IdOrderByCreatedAtAsc(promiseId)
                .stream()
                .map(PromiseSourceService::toResponse)
                .toList();
    }

    public static PromiseSourceResponse toResponse(PromiseSource source) {

        return new PromiseSourceResponse(
                source.getId(),
                source.getManifestoName(),
                source.getSourceUrl(),
                source.getPageNumber(),
                source.getExactQuotation(),
                source.getSourceType(),
                source.getPublishedDate(),
                source.getCreatedAt()
        );
    }
}
