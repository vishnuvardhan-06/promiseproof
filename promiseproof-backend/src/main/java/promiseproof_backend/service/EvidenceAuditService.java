package promiseproof_backend.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import promiseproof_backend.dto.EvidenceAuditResponse;
import promiseproof_backend.entity.Evidence;
import promiseproof_backend.entity.EvidenceAudit;
import promiseproof_backend.entity.User;
import promiseproof_backend.repository.EvidenceAuditRepository;
import promiseproof_backend.repository.UserRepository;

import java.util.List;

/**
 * Writes the append-only evidence audit trail and reads it back.
 */
@Service
public class EvidenceAuditService {

    private final EvidenceAuditRepository auditRepository;
    private final UserRepository userRepository;

    public EvidenceAuditService(
            EvidenceAuditRepository auditRepository,
            UserRepository userRepository) {

        this.auditRepository = auditRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void record(
            Evidence evidence,
            String action,
            String fromStatus,
            String toStatus,
            String comment) {

        EvidenceAudit audit = new EvidenceAudit();

        audit.setEvidenceId(evidence.getId());
        audit.setPromiseId(evidence.getPromise().getId());
        audit.setEvidenceTitle(evidence.getTitle());
        audit.setAction(action);
        audit.setFromStatus(fromStatus);
        audit.setToStatus(toStatus);
        audit.setComment(comment);

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null &&
                authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getName())) {

            String email = authentication.getName();

            audit.setActorEmail(email);

            userRepository.findByEmailIgnoreCase(email)
                    .map(User::getRole)
                    .ifPresent(role -> audit.setActorRole(role.name()));

        } else {
            audit.setActorEmail("system");
        }

        auditRepository.save(audit);
    }

    @Transactional(readOnly = true)
    public List<EvidenceAuditResponse> getForEvidence(Long evidenceId) {

        return auditRepository
                .findByEvidenceIdOrderByCreatedAtAsc(evidenceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvidenceAuditResponse> getRecent(int limit) {

        // Database-level pagination: only the requested number of rows is
        // fetched from the database.
        Page<EvidenceAudit> page = auditRepository
                .findByOrderByCreatedAtDesc(
                        PageRequest.of(
                                0,
                                limit,
                                Sort.by(Sort.Direction.DESC, "createdAt")
                        )
                );

        return page.getContent()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private EvidenceAuditResponse toResponse(EvidenceAudit audit) {

        return new EvidenceAuditResponse(
                audit.getId(),
                audit.getEvidenceId(),
                audit.getPromiseId(),
                audit.getEvidenceTitle(),
                audit.getAction(),
                audit.getFromStatus(),
                audit.getToStatus(),
                audit.getActorEmail(),
                audit.getActorRole(),
                audit.getComment(),
                audit.getCreatedAt()
        );
    }
}
