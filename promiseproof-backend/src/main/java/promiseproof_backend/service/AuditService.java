package promiseproof_backend.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import promiseproof_backend.dto.AuditRecordResponse;
import promiseproof_backend.entity.AuditRecord;
import promiseproof_backend.repository.AuditRecordRepository;

@Service
public class AuditService {

    private final AuditRecordRepository auditRepository;

    public AuditService(AuditRecordRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Transactional
    public void record(
            String action,
            String entityType,
            Long entityId,
            String entityTitle,
            String actorEmail,
            String actorRole,
            String comment
    ) {

        AuditRecord audit = new AuditRecord();

        audit.setAction(action);
        audit.setEntityType(entityType);
        audit.setEntityId(entityId);
        audit.setEntityTitle(entityTitle);
        audit.setActorEmail(actorEmail);
        audit.setActorRole(actorRole);
        audit.setComment(comment);

        auditRepository.save(audit);
    }

    @Transactional(readOnly = true)
    public List<AuditRecordResponse> getRecent(int limit) {

        Page<AuditRecord> page = auditRepository.findByOrderByCreatedAtDesc(
                PageRequest.of(
                        0,
                        limit,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                )
        );

        return page.getContent()
                .stream()
                .map(audit -> new AuditRecordResponse(
                        audit.getAction(),
                        audit.getEntityType(),
                        audit.getEntityId(),
                        audit.getEntityTitle(),
                        audit.getActorEmail(),
                        audit.getActorRole(),
                        audit.getComment(),
                        audit.getCreatedAt()
                ))
                .toList();
    }
}
