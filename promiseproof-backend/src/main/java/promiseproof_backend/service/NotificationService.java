package promiseproof_backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import promiseproof_backend.dto.NotificationInboxResponse;
import promiseproof_backend.dto.NotificationResponse;
import promiseproof_backend.entity.Notification;
import promiseproof_backend.entity.Role;
import promiseproof_backend.entity.User;
import promiseproof_backend.repository.NotificationRepository;
import promiseproof_backend.repository.UserRepository;

import java.util.List;

@Service
public class NotificationService {

    private static final int INBOX_LIMIT = 100;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository) {

        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void notifyUser(
            String email,
            String type,
            String title,
            String message,
            String link) {

        if (email == null || email.isBlank()) {
            return;
        }

        Notification notification = new Notification();

        notification.setRecipientEmail(email.trim().toLowerCase());
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLink(link);
        notification.setRead(false);

        notificationRepository.save(notification);
    }

    /**
     * Notify every active user with the given role, skipping the person who
     * performed the action so they do not notify themselves.
     */
    @Transactional
    public void notifyRole(
            Role role,
            String type,
            String title,
            String message,
            String link,
            String excludeEmail) {

        for (User user : userRepository.findByRole(role)) {

            if (!user.isEnabled()) {
                continue;
            }

            if (excludeEmail != null &&
                    excludeEmail.equalsIgnoreCase(user.getEmail())) {
                continue;
            }

            notifyUser(user.getEmail(), type, title, message, link);
        }
    }

    @Transactional(readOnly = true)
    public NotificationInboxResponse getInbox(String email) {

        List<NotificationResponse> items = notificationRepository
                .findByRecipientEmailIgnoreCaseOrderByCreatedAtDesc(email)
                .stream()
                .limit(INBOX_LIMIT)
                .map(this::toResponse)
                .toList();

        long unread = notificationRepository
                .countByRecipientEmailIgnoreCaseAndReadFalse(email);

        return new NotificationInboxResponse(items, unread);
    }

    @Transactional
    public void markRead(Long id, String email) {

        Notification notification = notificationRepository
                .findByIdAndRecipientEmailIgnoreCase(id, email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Notification not found"
                ));

        notification.setRead(true);

        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllRead(String email) {

        List<Notification> unread = notificationRepository
                .findByRecipientEmailIgnoreCaseAndReadFalse(email);

        unread.forEach(notification -> notification.setRead(true));

        notificationRepository.saveAll(unread);
    }

    private NotificationResponse toResponse(Notification notification) {

        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getLink(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
