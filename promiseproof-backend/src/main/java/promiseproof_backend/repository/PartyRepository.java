package promiseproof_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import promiseproof_backend.entity.Party;

public interface PartyRepository extends JpaRepository<Party, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}