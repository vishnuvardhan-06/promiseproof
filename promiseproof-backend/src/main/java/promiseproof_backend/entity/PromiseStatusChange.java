package promiseproof_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Immutable history of promise status transitions that were applied through
 * an approved status verification proposal. Old and new status values are both
 * retained for every applied transition.
 */
@Entity
@Table(name = "promise_status_changes")
public class PromiseStatusChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long promiseId;

    @Column(length = 20)
    private String fromStatus;

    @Column(length = 20)
    private String toStatus;

    @Column(length = 190)
    private String changedByEmail;

    @Column(length = 120)
    private String changedByName;

    @Column(length = 2000)
    private String reason;

    /**
     * Optional link to the approved evidence that supported this transition.
     * Used only for the public supporting-evidence reference.
     */
    private Long evidenceId;

    /**
     * Set to APPROVED when an independent status-review proposal was accepted.
     * Only independently approved transitions are exposed in public history;
     * administrator corrections intentionally remain internal.
     */
    @Column(length = 20)
    private String reviewerDecision;

    private LocalDateTime createdAt;

    public PromiseStatusChange() {
    }

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPromiseId() {
        return promiseId;
    }

    public void setPromiseId(Long promiseId) {
        this.promiseId = promiseId;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public String getChangedByEmail() {
        return changedByEmail;
    }

    public void setChangedByEmail(String changedByEmail) {
        this.changedByEmail = changedByEmail;
    }

    public String getChangedByName() {
        return changedByName;
    }

    public void setChangedByName(String changedByName) {
        this.changedByName = changedByName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getEvidenceId() {
        return evidenceId;
    }

    public void setEvidenceId(Long evidenceId) {
        this.evidenceId = evidenceId;
    }

    public String getReviewerDecision() {
        return reviewerDecision;
    }

    public void setReviewerDecision(String reviewerDecision) {
        this.reviewerDecision = reviewerDecision;
    }
}
