package promiseproof_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Immutable record of an important action taken on an evidence document.
 *
 * Evidence references are stored as plain values (rather than foreign keys)
 * so that the accountability history survives record changes. Audit rows are
 * never updated or deleted through the application.
 */
@Entity
@Table(name = "evidence_audits")
public class EvidenceAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long evidenceId;

    @Column(nullable = false)
    private Long promiseId;

    @Column(length = 200)
    private String evidenceTitle;

    // UPLOADED, APPROVED, REJECTED
    @Column(nullable = false, length = 30)
    private String action;

    @Column(length = 20)
    private String fromStatus;

    @Column(length = 20)
    private String toStatus;

    @Column(length = 190)
    private String actorEmail;

    @Column(length = 20)
    private String actorRole;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public EvidenceAudit() {
    }

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getEvidenceId() {
        return evidenceId;
    }

    public void setEvidenceId(Long evidenceId) {
        this.evidenceId = evidenceId;
    }

    public Long getPromiseId() {
        return promiseId;
    }

    public void setPromiseId(Long promiseId) {
        this.promiseId = promiseId;
    }

    public String getEvidenceTitle() {
        return evidenceTitle;
    }

    public void setEvidenceTitle(String evidenceTitle) {
        this.evidenceTitle = evidenceTitle;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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

    public String getActorEmail() {
        return actorEmail;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }

    public String getActorRole() {
        return actorRole;
    }

    public void setActorRole(String actorRole) {
        this.actorRole = actorRole;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
