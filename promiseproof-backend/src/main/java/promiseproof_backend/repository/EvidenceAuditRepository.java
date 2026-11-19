package promiseproof_backend.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import promiseproof_backend.entity.EvidenceAudit;

public interface EvidenceAuditRepository
        extends JpaRepository<EvidenceAudit, Long> {

    List<EvidenceAudit> findByEvidenceIdOrderByCreatedAtAsc(Long evidenceId);

    Page<EvidenceAudit> findByOrderByCreatedAtDesc(Pageable pageable);
}
