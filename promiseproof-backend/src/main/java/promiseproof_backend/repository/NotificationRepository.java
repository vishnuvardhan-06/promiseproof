package promiseproof_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import promiseproof_backend.entity.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientEmailIgnoreCaseOrderByCreatedAtDesc(
            String recipientEmail
    );

    List<Notification> findByRecipientEmailIgnoreCaseAndReadFalse(
            String recipientEmail
    );

    long countByRecipientEmailIgnoreCaseAndReadFalse(String recipientEmail);

    Optional<Notification> findByIdAndRecipientEmailIgnoreCase(
            Long id,
            String recipientEmail
    );
}
