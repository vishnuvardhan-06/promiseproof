
package promiseproof_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import promiseproof_backend.entity.Election;

public interface ElectionRepository
        extends JpaRepository<Election, Long> {

    boolean existsByNameIgnoreCaseAndStateIgnoreCaseAndYear(
            String name, String state, Integer year
    );

    boolean existsByNameIgnoreCaseAndStateIgnoreCaseAndYearAndIdNot(
            String name, String state, Integer year, Long id
    );
}