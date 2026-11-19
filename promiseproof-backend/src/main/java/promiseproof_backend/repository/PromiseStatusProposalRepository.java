package promiseproof_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import promiseproof_backend.entity.PromiseStatusProposal;

public interface PromiseStatusProposalRepository
        extends JpaRepository<PromiseStatusProposal, Long> {

    Optional<PromiseStatusProposal> findFirstByPromiseIdAndStatusOrderByCreatedAtDesc(
            Long promiseId,
            String status
    );

    List<PromiseStatusProposal> findByStatusOrderByCreatedAtDesc(String status);

    List<PromiseStatusProposal> findByPromiseIdOrderByCreatedAtDesc(Long promiseId);
}
