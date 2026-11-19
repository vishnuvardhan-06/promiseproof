package promiseproof_backend.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import promiseproof_backend.entity.AuditRecord;

public interface AuditRecordRepository
        extends JpaRepository<AuditRecord, Long> {

    List<AuditRecord> findAllByOrderByCreatedAtDesc();

    Page<AuditRecord> findByOrderByCreatedAtDesc(Pageable pageable);
}
