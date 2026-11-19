package promiseproof_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import promiseproof_backend.entity.PromiseSource;

public interface PromiseSourceRepository
        extends JpaRepository<PromiseSource, Long> {

    List<PromiseSource> findByPromise_IdOrderByCreatedAtAsc(Long promiseId);

    long countByPromise_Id(Long promiseId);
}
