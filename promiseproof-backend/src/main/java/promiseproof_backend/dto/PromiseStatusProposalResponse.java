package promiseproof_backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class PromiseStatusProposalResponse {

    private Long id;
    private Long promiseId;
    private String promiseTitle;
    private String promiseDescription;
    private String promisePartyName;
    private String promiseElectionName;
    private String currentStatus;
    private String proposedStatus;
    private String reason;
    private Long evidenceId;
    private String evidenceTitle;
    private String evidenceType;
    private String evidenceSourceUrl;
    private String evidenceFileName;
    private String evidenceStatus;
    private String evidenceUploadedByName;
    private String evidenceUploadedByEmail;
    private String proposedByEmail;
    private String proposedByName;
    private String status;
    private String reviewComment;
    private List<PromiseStatusHistoryResponse> statusHistory;
    private LocalDateTime createdAt;
    private LocalDateTime decidedAt;

    public PromiseStatusProposalResponse() {
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

    public String getPromiseTitle() {
        return promiseTitle;
    }

    public void setPromiseTitle(String promiseTitle) {
        this.promiseTitle = promiseTitle;
    }

    public String getPromiseDescription() {
        return promiseDescription;
    }

    public void setPromiseDescription(String promiseDescription) {
        this.promiseDescription = promiseDescription;
    }

    public String getPromisePartyName() {
        return promisePartyName;
    }

    public void setPromisePartyName(String promisePartyName) {
        this.promisePartyName = promisePartyName;
    }

    public String getPromiseElectionName() {
        return promiseElectionName;
    }

    public void setPromiseElectionName(String promiseElectionName) {
        this.promiseElectionName = promiseElectionName;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
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

    public String getEvidenceTitle() {
        return evidenceTitle;
    }

    public void setEvidenceTitle(String evidenceTitle) {
        this.evidenceTitle = evidenceTitle;
    }

    public String getEvidenceType() {
        return evidenceType;
    }

    public void setEvidenceType(String evidenceType) {
        this.evidenceType = evidenceType;
    }

    public String getEvidenceSourceUrl() {
        return evidenceSourceUrl;
    }

    public void setEvidenceSourceUrl(String evidenceSourceUrl) {
        this.evidenceSourceUrl = evidenceSourceUrl;
    }

    public String getEvidenceFileName() {
        return evidenceFileName;
    }

    public void setEvidenceFileName(String evidenceFileName) {
        this.evidenceFileName = evidenceFileName;
    }

    public String getEvidenceStatus() {
        return evidenceStatus;
    }

    public void setEvidenceStatus(String evidenceStatus) {
        this.evidenceStatus = evidenceStatus;
    }

    public String getEvidenceUploadedByName() {
        return evidenceUploadedByName;
    }

    public void setEvidenceUploadedByName(String evidenceUploadedByName) {
        this.evidenceUploadedByName = evidenceUploadedByName;
    }

    public String getEvidenceUploadedByEmail() {
        return evidenceUploadedByEmail;
    }

    public void setEvidenceUploadedByEmail(String evidenceUploadedByEmail) {
        this.evidenceUploadedByEmail = evidenceUploadedByEmail;
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

    public List<PromiseStatusHistoryResponse> getStatusHistory() {
        return statusHistory;
    }

    public void setStatusHistory(
            List<PromiseStatusHistoryResponse> statusHistory) {
        this.statusHistory = statusHistory;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }
}
