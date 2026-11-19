package promiseproof_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A researcher's proposal to change a promise's public status.
 *
 * A status change only reaches the public record after a reviewer (or an
 * administrator acting as a reviewer) approves the proposal. The proposal
 * references an approved evidence document that supports the new status.
 */
@Entity
@Table(name = "promise_status_proposals")
public class PromiseStatusProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long promiseId;

    @Column(nullable = false, length = 20)
    private String proposedStatus;

    @Column(columnDefinition = "TEXT")
    private String reason;

    private Long evidenceId;

    @Column(length = 190)
    private String proposedByEmail;

    @Column(length = 120)
    private String proposedByName;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(columnDefinition = "TEXT")
    private String reviewComment;

    private LocalDateTime createdAt;

    private LocalDateTime decidedAt;

    public PromiseStatusProposal() {
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

    public String getProposedStatus() {
        return proposedStatus;
    }

    public void setProposedStatus(String proposedStatus) {
        this.proposedStatus = proposedStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getEvidenceId() {
        return evidenceId;
    }

    public void setEvidenceId(Long evidenceId) {
        this.evidenceId = evidenceId;
    }

    public String getProposedByEmail() {
        return proposedByEmail;
    }

    public void setProposedByEmail(String proposedByEmail) {
        this.proposedByEmail = proposedByEmail;
    }

    public String getProposedByName() {
        return proposedByName;
    }

    public void setProposedByName(String proposedByName) {
        this.proposedByName = proposedByName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }
}
