package promiseproof_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import promiseproof_backend.entity.PromiseStatusChange;

public interface PromiseStatusChangeRepository
        extends JpaRepository<PromiseStatusChange, Long> {

    List<PromiseStatusChange> findByPromiseIdOrderByCreatedAtAsc(Long promiseId);
}
