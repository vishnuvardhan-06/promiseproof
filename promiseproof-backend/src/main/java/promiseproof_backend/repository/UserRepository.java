package promiseproof_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import promiseproof_backend.entity.Role;
import promiseproof_backend.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByRole(Role role);

    long countByRoleAndEnabledTrue(Role role);
}
