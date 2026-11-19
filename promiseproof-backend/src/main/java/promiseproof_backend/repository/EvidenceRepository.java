
package promiseproof_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import promiseproof_backend.entity.Evidence;

import java.util.List;

@Repository
public interface EvidenceRepository
        extends JpaRepository<Evidence, Long> {

    List<Evidence> findByPromise_IdOrderByUploadedAtDesc(Long promiseId);

    List<Evidence> findByPromise_IdAndStatusOrderByUploadedAtDesc(
            Long promiseId,
            String status
    );

    List<Evidence> findByUploadedByEmailIgnoreCaseOrderByUploadedAtDesc(
            String uploadedByEmail
    );

    List<Evidence> findByStatusOrderByUploadedAtDesc(String status);

    long countByPromise_Id(Long promiseId);

    long countByPromise_IdAndStatus(Long promiseId, String status);

    long countByStatus(String status);
}
