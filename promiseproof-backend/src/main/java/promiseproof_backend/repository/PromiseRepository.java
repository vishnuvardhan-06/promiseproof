package promiseproof_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import promiseproof_backend.entity.Promise;

import java.util.List;

@Repository
public interface PromiseRepository
        extends JpaRepository<Promise, Long>,
        JpaSpecificationExecutor<Promise> {

    long countByParty_Id(Long partyId);

    long countByElection_Id(Long electionId);

    long countByStatus(String status);

    List<Promise> findByCreatedByEmailIgnoreCaseOrderByCreatedAtDesc(
            String createdByEmail
    );
}
